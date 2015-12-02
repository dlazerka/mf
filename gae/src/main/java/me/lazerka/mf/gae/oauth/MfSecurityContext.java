package me.lazerka.mf.gae.oauth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;

import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

/**
 * @author Dzmitry Lazerka
 */
public class MfSecurityContext implements SecurityContext {
	private final GoogleIdToken token;

	public MfSecurityContext(GoogleIdToken token) {
		this.token = token;
	}

	@Override
	public Principal getUserPrincipal() {
		return new MfOAuthPrincipal(token);
	}

	@Override
	public boolean isUserInRole(String role) {
		return token.getPayload().getAudienceAsList().contains(role);
	}

	@Override
	public boolean isSecure() {
		// AuthFilter rejects insecure requests.
		return true;
	}

	@Override
	public String getAuthenticationScheme() {
		return "oauth2";
	}

}
