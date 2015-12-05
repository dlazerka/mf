package me.lazerka.mf.gae.oauth;

import javax.annotation.Nonnull;
import java.security.Principal;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dzmitry Lazerka
 */
public class OauthUser implements Principal {
	private final String id;
	private final String email;

	public OauthUser(@Nonnull String id, @Nonnull String email) {
		this.id = checkNotNull(id);
		this.email = checkNotNull(email);
	}

	@Override
	public String getName() {
		return id;
	}

	public String getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}
}
