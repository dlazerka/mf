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

import javax.annotation.Nonnull;
import java.util.AbstractList;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Interleaved representation of two lists of the same size.
 *
 * @author Dzmitry Lazerka
 */
public class PairedList<A, B> extends AbstractList<Pair<A, B>> {
	private final List<A> first;
	private final List<B> second;

	public PairedList(@Nonnull List<A> first, @Nonnull List<B> second) {
		this.first = checkNotNull(first);
		this.second = checkNotNull(second);

		if (first.size() != second.size()) {
			throw new IllegalArgumentException("Sizes not equal: " + first.size() + " vs " + second.size());
		}
	}

	@Override
	public Pair<A, B> get(int index) {
		return Pair.of(first.get(index), second.get(index));
	}

	@Override
	public int size() {
		return first.size();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		PairedList<?, ?> that = (PairedList<?, ?>) o;
		return Objects.equals(first, that.first) &&
				Objects.equals(second, that.second);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), first, second);
	}
}
