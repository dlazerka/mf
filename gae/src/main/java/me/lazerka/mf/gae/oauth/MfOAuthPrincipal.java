package me.lazerka.mf.gae.oauth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;

import java.security.Principal;

/**
 * @author Dzmitry Lazerka
 */
public class MfOAuthPrincipal implements Principal {
	private final GoogleIdToken token;

	public MfOAuthPrincipal(GoogleIdToken token) {
		this.token = token;
	}

	@Override
	public String getName() {
		// Note not email.
		return token.getPayload().getSubject();
	}

	public GoogleIdToken getToken() {
		return token;
	}

	public String getEmail() {
		return token.getPayload().getEmail();
	}
}
