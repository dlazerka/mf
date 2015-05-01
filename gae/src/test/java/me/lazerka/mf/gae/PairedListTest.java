package me.lazerka.mf.gae;

import com.google.common.collect.ImmutableList;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class PairedListTest {
	@Test
	public void test() throws Exception {
		List<String> a = ImmutableList.of("A1", "A2");
		List<Integer> b = ImmutableList.of(1, 2);

		PairedList<String, Integer> pairedList = new PairedList<>(a, b);

		List<Pair<String, Integer>> actual = new ArrayList<>();
		for(Pair<String, Integer> pair : pairedList) {
			actual.add(pair);
		}

		List<Pair<String, Integer>> expected = ImmutableList.of(
				Pair.of("A1", 1),
				Pair.of("A2", 2)
		);

		assertEquals(actual, expected);
	}
}
