package org.apache.pig.piggybank.evaluation.util;


import java.io.IOException;

import org.apache.pig.EvalFunc;
import org.apache.pig.data.BagFactory;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;

/**
 * This class takes a list of items and puts them into a bag
 * 
 * T = foreach U generate ToBag($0, $1, $2);
 * 
 * It's like saying this:
 * 
 * T = foreach U generate {($0), ($1), ($2)}
 *
 */
public class ToBag extends EvalFunc<DataBag> {

	@Override
	public DataBag exec(Tuple input) throws IOException {
		try {
			DataBag bag = BagFactory.getInstance().newDefaultBag();

			for (int i = 0; i < input.size(); ++i) {
				final Object object = input.get(i);
				if (object != null) {
					Tuple tp2 = TupleFactory.getInstance().newTuple(1);
					tp2.set(0, object);
					bag.add(tp2);
				}
			}

			return bag;
		} catch (Exception ee) {
			throw new RuntimeException("Error while creating a bag", ee);
		}
	}

}
