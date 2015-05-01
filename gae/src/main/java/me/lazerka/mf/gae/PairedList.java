package me.lazerka.mf.gae;

import javax.annotation.Nonnull;
import java.util.*;

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
