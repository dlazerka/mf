package me.lazerka.mf.gae.oauth;

import javax.annotation.Nonnull;
import javax.ws.rs.core.SecurityContext;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dzmitry Lazerka
 */
public class OauthSecurityContext implements SecurityContext {
	@Nonnull
	private final OauthUser user;

	public OauthSecurityContext(@Nonnull OauthUser user, boolean isSecure) {
		this.user = checkNotNull(user);
		checkArgument(isSecure, "OAuth2.0 should always be secure.");
	}

	@Override
	public OauthUser getUserPrincipal() {
		return user;
	}

	@Override
	public boolean isUserInRole(String role) {
		//return token.getPayload().getAudienceAsList().contains(role);
		return Role.OAUTH.equals(role);
	}

	@Override
	public boolean isSecure() {
		return true;
	}

	@Override
	public String getAuthenticationScheme() {
		return "OAuth2.0";
	}

}
