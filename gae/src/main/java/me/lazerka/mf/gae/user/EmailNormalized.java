package me.lazerka.mf.gae.user;

import javax.annotation.Nonnull;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * See {@link UserService#normalizeEmail}.
 *
 * @author Dzmitry Lazerka
 */
public class EmailNormalized {
	@Nonnull
	private final String email;

	public EmailNormalized(@Nonnull String email) {
		this.email = checkNotNull(email);
	}

	@Nonnull
	public String getEmail() {
		return email;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EmailNormalized that = (EmailNormalized) o;
		return Objects.equals(email, that.email);
	}

	@Override
	public int hashCode() {
		return Objects.hash(email);
	}

	@Override
	public String toString() {
		return email;
	}
}
