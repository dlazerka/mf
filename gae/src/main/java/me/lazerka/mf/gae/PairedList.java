package me.lazerka.mf.gae;

import javax.annotation.Nonnull;
import java.util.AbstractList;
import java.util.List;

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
		return first.size() * 2;
	}
}
