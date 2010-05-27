package org.paache.pig.piggybank.util;

import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import org.apache.pig.data.DataBag;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;
import org.apache.pig.piggybank.evaluation.util.ToBag;
import org.apache.pig.piggybank.evaluation.util.ToTuple;
import org.junit.Test;

public class TestToBagToTuple {
	@Test
	public void toBag() throws Exception{
		ToBag tb = new ToBag();

		Tuple input = TupleFactory.getInstance().newTuple();
		for (int i = 0; i < 100; ++i) {
			input.append(i);
		}

		Set<Integer> s = new HashSet<Integer>();
		DataBag db = tb.exec(input);
		for (Tuple t : db) {
			s.add((Integer) t.get(0));
		}

		// finally check the bag had everything we put in the tuple.
		Assert.assertEquals(100, s.size());
		for (int i = 0; i < 100; ++i) {
			Assert.assertTrue(s.contains(i));
		}
	}

	@Test
	public void toTuple() throws Exception{
		ToTuple tb = new ToTuple();

		Tuple input = TupleFactory.getInstance().newTuple();
		for (int i = 0; i < 100; ++i) {
			input.append(i);
		}

		Tuple output = tb.exec(input);
		Assert.assertFalse(input == output);
		Assert.assertEquals(input, output);
	}
}
