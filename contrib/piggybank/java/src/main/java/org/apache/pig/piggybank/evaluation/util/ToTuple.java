package org.apache.pig.piggybank.evaluation.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pig.EvalFunc;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;
import org.apache.pig.impl.logicalLayer.schema.Schema;

/**
 * This class makes a tuple out of the parameter
 *
 * T = foreach U generate ToTuple($0, $1, $2);
 * 
 * It generates a tuple containing $0, $1, and $2
 *
 *
 */
public class ToTuple extends EvalFunc<Tuple> {

	@Override
	public Tuple exec(Tuple input) throws IOException {
		try {
			List<Object> items = new ArrayList<Object>();
			for (int i = 0; i < input.size(); ++i) {
				items.add(input.get(i));
			}
			return TupleFactory.getInstance().newTuple(items);
		} catch (Exception e) {
			throw new RuntimeException("Error while creating a tuple", e);
		}
	}

	@Override
	public Schema outputSchema(Schema input) {
		try {
			Schema tupleSchema = new Schema();
			for (int i = 0; i < input.size(); ++i) {
				tupleSchema.add(input.getField(i));
			}
			return new Schema(new Schema.FieldSchema(getSchemaName(this
					.getClass().getName().toLowerCase(), input), tupleSchema,
					DataType.TUPLE));
		} catch (Exception e) {
			return null;
		}
	}

}
