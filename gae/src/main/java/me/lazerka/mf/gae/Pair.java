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

import java.util.Objects;

/**
 * @author Dzmitry Lazerka
 */
public class Pair<A, B> {
	private final A first;
	private final B second;

	public Pair(A first, B second) {
		this.first = first;
		this.second = second;
	}

	public static <A, B> Pair<A, B> of(A first, B second) {
		return new Pair<>(first, second);
	}

	public A getFirst() {
		return first;
	}

	public B getSecond() {
		return second;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Pair<?, ?> pair = (Pair<?, ?>) o;
		return Objects.equals(first, pair.first) &&
				Objects.equals(second, pair.second);
	}

	@Override
	public int hashCode() {
		return Objects.hash(first, second);
	}

	@Override
	public String toString() {
		return "(" + first + ", " + second + ')';
	}
}
