package me.lazerka.mf.gae.oauth;

import javax.ws.rs.core.SecurityContext;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dzmitry Lazerka
 */
public class AuthSecurityContext implements SecurityContext {
	private final UserPrincipal user;
	private final boolean secure;
	private final Set<String> roles;
	private final String authenticationScheme;

	public AuthSecurityContext(UserPrincipal user, boolean secure, Set<String> role, String authenticationScheme) {
		this.user = checkNotNull(user);
		this.secure = secure;
		this.roles = checkNotNull(role);
		this.authenticationScheme = checkNotNull(authenticationScheme);
	}

	@Override
	public UserPrincipal getUserPrincipal() {
		return user;
	}

	@Override
	public boolean isUserInRole(String role) {
		return roles.contains(role);
	}

	@Override
	public boolean isSecure() {
		return secure;
	}

	@Override
	public String getAuthenticationScheme() {
		return authenticationScheme;
	}
}
