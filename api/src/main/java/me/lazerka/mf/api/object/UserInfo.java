package me.lazerka.mf.api.object;

import javax.annotation.Nonnull;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dzmitry Lazerka
 */
public class UserInfo {
	@Nonnull
	private final String email;

	public UserInfo(@Nonnull String email) {
		this.email = checkNotNull(email);
	}

	/** Canonicalized on server side. May differ from user's contacts. */
	@Nonnull
	public String getEmail() {
		return email;
	}

	@Override
	public String toString() {
		return email;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		UserInfo userInfo = (UserInfo) o;
		return Objects.equals(email, userInfo.email);
	}

	@Override
	public int hashCode() {
		return Objects.hash(email);
	}
}
