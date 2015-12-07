package me.lazerka.mf.gae.oauth;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.utils.SystemProperty;
import com.google.appengine.api.utils.SystemProperty.Environment.Value;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Set;

import static com.google.appengine.api.utils.SystemProperty.Environment.Value.Development;
import static com.google.common.base.Preconditions.checkNotNull;
import static me.lazerka.mf.api.ApiConstants.COOKIE_NAME_AUTH_TOKEN;

/**
 * Checks requests whether they are authenticated using either GAE or OAuth authentication.
 *
 * @see <a href="https://developers.google.com/identity/sign-in/android/backend-auth">documentation</a>.
 * @author Dzmitry Lazerka
 */
public class AuthFilter implements ResourceFilter, ContainerRequestFilter {
	private static final Logger logger = LoggerFactory.getLogger(AuthFilter.class);

	@Inject
	UserService userService;

	@Inject
	TokenVerifier tokenVerifier;

	Set<String> rolesAllowed;

	protected void setRolesAllowed(Set<String> rolesAllowed) {
		this.rolesAllowed = rolesAllowed;
	}

	private boolean isDevServer() {
		Value env = SystemProperty.environment.value();
		return env.equals(Development);
	}

	@Override
	@Nonnull
	public ContainerRequest filter(ContainerRequest request) {
		checkNotNull(rolesAllowed);

		AuthSecurityContext securityContext = getSecurityContext(request);

		for(String roleAllowed : rolesAllowed) {
			if (securityContext.isUserInRole(roleAllowed)) {
				request.setSecurityContext(securityContext);
				return request;
			}
		}

		UserPrincipal principal = securityContext.getUserPrincipal();
		logger.warn("User {} not in roles {}", principal, rolesAllowed);

		throw new WebApplicationException(getForbiddenResponse(request, "Not Authorized"));
	}

	private AuthSecurityContext getSecurityContext(ContainerRequest request) {
		// Deny all insecure requests on production (@PermitAll requests do not come here at all).
		if (!request.isSecure() && !isDevServer()) {
			logger.warn("Insecure auth, Deny: " + request);
			throw new WebApplicationException(getForbiddenResponse(request, "Request insecure"));
		}

		// Check regular GAE authentication.
		if (userService.isUserLoggedIn()) {
			return useGaeAuthentication(request);
		}

		// Check OAuth authentication.
		Cookie cookie = request.getCookies().get(COOKIE_NAME_AUTH_TOKEN);
		if (cookie != null) {
			return useOauthAuthentication(request, cookie);
		}

		logger.warn("No credentials provided for {}", request.getPath());
		throw new WebApplicationException(getForbiddenResponse(request, "No credentials provided"));
	}

	private AuthSecurityContext useOauthAuthentication(ContainerRequest request, Cookie cookie) {
		logger.trace("Authenticating OAuth2.0 user...");
		try {
			UserPrincipal userPrincipal = tokenVerifier.verify(cookie.getValue());
			return new AuthSecurityContext(
					userPrincipal,
					request.isSecure(),
					ImmutableSet.of(Role.USER),
					"OAuth2.0"
			);
		} catch (GeneralSecurityException e) {
			logger.info("Invalid token", e);
			throw new WebApplicationException(e, getForbiddenResponse(request, "Invalid OAuth2.0 token"));
		} catch (IOException e) {
			logger.error("IOException verifying OAuth token", e);
			throw new WebApplicationException(e, getForbiddenResponse(request, "Error verifying OAuth2.0 token"));
		}
	}

	private AuthSecurityContext useGaeAuthentication(ContainerRequest request) {
		Set<String> roles = userService.isUserAdmin()
				? ImmutableSet.of(Role.USER, Role.ADMIN)
				: ImmutableSet.of(Role.USER);

		User user = userService.getCurrentUser();
		UserPrincipal userPrincipal = new UserPrincipal(user.getUserId(), user.getEmail());
		return new AuthSecurityContext(
				userPrincipal,
				request.isSecure(),
				roles,
				"GAE"
		);
	}

	private Response getForbiddenResponse(ContainerRequest request, String reason) {
		// In case request is AJAX, we want to tell client how to authenticate user.
		String loginUrl = composeLoginUrl(request);

		return Response
				.status(Status.FORBIDDEN)
				.type(MediaType.TEXT_PLAIN_TYPE)
				.header("X-Login-URL", loginUrl)
				.entity(reason)
				.build();
	}

	private String composeLoginUrl(ContainerRequest request) {
		List<String> loginReturnUrls = request.getRequestHeader("X-Login-Return-Url");
		String loginReturnUrl;
		if (loginReturnUrls == null || loginReturnUrls.isEmpty()) {
			loginReturnUrl = request.getRequestUri().toASCIIString();
		} else {
			loginReturnUrl = URI.create(loginReturnUrls.get(0)).toASCIIString();
		}
		return userService.createLoginURL(loginReturnUrl);
	}

	@Override
	public ContainerRequestFilter getRequestFilter() {
		return this;
	}

	@Override
	public ContainerResponseFilter getResponseFilter() {
		return null;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("rolesAllowed", rolesAllowed)
				.toString();
	}
}
