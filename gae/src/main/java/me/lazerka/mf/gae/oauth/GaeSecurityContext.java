package me.lazerka.mf.gae.oauth;

import javax.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dzmitry Lazerka
 */
public class GaeSecurityContext implements SecurityContext {
	private final UserPrincipal user;
	private final boolean secure;
	private final Set<String> roles;

	public GaeSecurityContext(UserPrincipal user, boolean secure, Set<String> role) {
		this.user = checkNotNull(user);
		this.secure = secure;
		this.roles = checkNotNull(role);
	}

	@Override
	public Principal getUserPrincipal() {
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
		return "GAE";
	}
}
