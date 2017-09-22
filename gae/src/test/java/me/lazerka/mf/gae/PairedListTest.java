/*
 *     Copyright (C) 2017 Dzmitry Lazerka
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

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
