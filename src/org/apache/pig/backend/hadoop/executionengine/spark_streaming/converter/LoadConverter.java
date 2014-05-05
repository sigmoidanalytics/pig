package org.apache.pig.backend.hadoop.executionengine.spark_streaming.converter;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.pig.LoadFunc;
import org.apache.pig.backend.hadoop.executionengine.mapReduceLayer.PigInputFormat;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.PhysicalOperator;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.plans.PhysicalPlan;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.relationalOperators.POLoad;
import org.apache.pig.backend.hadoop.executionengine.spark_streaming.SparkUtil;
import org.apache.pig.data.DefaultTuple;
import org.apache.pig.data.Tuple;
import org.apache.pig.impl.PigContext;
import org.apache.pig.impl.io.FileSpec;
import org.apache.pig.impl.plan.OperatorKey;
import org.apache.pig.impl.util.ObjectSerializer;
import org.apache.spark.streaming.DStream;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;

import scala.Function1;
import scala.Tuple2;
import scala.reflect.ClassManifest;
import scala.runtime.AbstractFunction1;

import com.google.common.collect.Lists;

/**
 * Converter that loads data via POLoad and converts it to RRD&lt;Tuple>. Abuses the interface a bit
 * in that there is no inoput RRD to convert in this case. Instead input is the source path of the
 * POLoad.
 *
 * @author billg
 */
@SuppressWarnings({ "serial"})
public class LoadConverter implements POConverter<Tuple, Tuple, POLoad> {

	private static final Function1<Tuple2<Text, Tuple>, Tuple> TO_VALUE_FUNCTION = new ToTupleFunction();
     
    private PigContext pigContext;
    private PhysicalPlan physicalPlan;
    private JavaStreamingContext sparkContext;

    public LoadConverter(PigContext pigContext, PhysicalPlan physicalPlan, JavaStreamingContext sparkContext2) {
        this.pigContext = pigContext;
        this.physicalPlan = physicalPlan;
        this.sparkContext = sparkContext2;
    }

    @Override
    public JavaDStream<Tuple> convert(List<JavaDStream<Tuple>> predecessorRdds, POLoad poLoad) throws IOException {
//        if (predecessors.size()!=0) {
//            throw new RuntimeException("Should not have predecessors for Load. Got : "+predecessors);
//        }
    		configureLoader(physicalPlan, poLoad, sparkContext.ssc().sc().hadoopConfiguration(),this.pigContext);
        DStream<Tuple2<Text, Tuple>> hadoopRDD= sparkContext.ssc().fileStream(poLoad.getLFile().getFileName(), 
				SparkUtil.getManifest(Text.class), 
				SparkUtil.getManifest(Tuple.class), 
				SparkUtil.getManifest(PigInputFormat.class));
        // map to get just RDD<Tuple>
        return new JavaDStream<Tuple>(hadoopRDD.map(TO_VALUE_FUNCTION,SparkUtil.getManifest(Tuple.class)), 
        		SparkUtil.getManifest(Tuple.class));
    }

    private static class ToTupleFunction extends AbstractFunction1<Tuple2<Text, Tuple>,Tuple>
            implements Function1<Tuple2<Text, Tuple>,Tuple>, Serializable {

        @Override
        public Tuple apply(Tuple2<Text, Tuple> v1) {
        	return v1._2;
        }
    }

    /**
     * stolen from JobControlCompiler
     * TODO: refactor it to share this
     * @param physicalPlan
     * @param poLoad
     * @param configuration
     * @return
     * @throws java.io.IOException
     */
    private static Configuration configureLoader(PhysicalPlan physicalPlan,
                                           POLoad poLoad, Configuration configuration, PigContext pigContext) throws IOException {

        Job job = new Job(configuration);
        LoadFunc loadFunc = poLoad.getLoadFunc();

        loadFunc.setLocation(poLoad.getLFile().getFileName(), job);

        // stolen from JobControlCompiler
        ArrayList<FileSpec> pigInputs = new ArrayList<FileSpec>();
        //Store the inp filespecs
        pigInputs.add(poLoad.getLFile());

        ArrayList<List<OperatorKey>> inpTargets = Lists.newArrayList();
        ArrayList<String> inpSignatures = Lists.newArrayList();
        ArrayList<Long> inpLimits = Lists.newArrayList();
        //Store the target operators for tuples read
        //from this input
        List<PhysicalOperator> loadSuccessors = physicalPlan.getSuccessors(poLoad);
        List<OperatorKey> loadSuccessorsKeys = Lists.newArrayList();
        if(loadSuccessors!=null){
            for (PhysicalOperator loadSuccessor : loadSuccessors) {
                loadSuccessorsKeys.add(loadSuccessor.getOperatorKey());
            }
        }
        inpTargets.add(loadSuccessorsKeys);
        inpSignatures.add(poLoad.getSignature());
        inpLimits.add(poLoad.getLimit());

        configuration.set("pig.inputs", ObjectSerializer.serialize(pigInputs));
        configuration.set("pig.inpTargets", ObjectSerializer.serialize(inpTargets));
        configuration.set("pig.inpSignatures", ObjectSerializer.serialize(inpSignatures));
        configuration.set("pig.inpLimits", ObjectSerializer.serialize(inpLimits));
        configuration.set("pig.pigContext", ObjectSerializer.serialize(pigContext));
        configuration.set("udf.import.list", ObjectSerializer.serialize(PigContext.getPackageImportList()));
        return configuration;
    }
}
