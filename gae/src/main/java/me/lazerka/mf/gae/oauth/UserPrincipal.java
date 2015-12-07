package me.lazerka.mf.gae.oauth;

import javax.annotation.Nonnull;
import java.security.Principal;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dzmitry Lazerka
 */
public class UserPrincipal implements Principal {
	private final String id;
	private final String email;

	public UserPrincipal(@Nonnull String id, @Nonnull String email) {
		this.id = checkNotNull(id);
		this.email = checkNotNull(email);
		checkArgument(email.indexOf('@') != -1, "Wrong order of arguments");
	}

	@Nonnull
	@Override
	public String getName() {
		return id;
	}

	@Nonnull
	public String getId() {
		return id;
	}

	@Nonnull
	public String getEmail() {
		return email;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		UserPrincipal that = (UserPrincipal) o;

		return id.equals(that.id);

	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public String toString() {
		return email + ' ' + id;
	}
}
