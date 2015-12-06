package me.lazerka.mf.gae.oauth;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.utils.SystemProperty;
import com.google.appengine.api.utils.SystemProperty.Environment.Value;
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
import java.util.Map;
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

	Set<String> roles;

	protected void setRoles(Set<String> roles) {
		this.roles = roles;
	}

	private boolean isDevServer() {
		Value env = SystemProperty.environment.value();
		return env.equals(Development);
	}

	@Override
	@Nonnull
	public ContainerRequest filter(ContainerRequest request) {
		checkNotNull(roles);

		// Allow HTTP on local dev server only.
		if (roles.contains(Role.DEVSERVER) && isDevServer() && userService.isUserLoggedIn()) {
			logger.info("Dev auth, OK: " + request);
			setGaeSecurityContext(request, Role.DEVSERVER);
			return request;
		}

		// Deny all insecure requests (@PermitAll requests do not come here at all).
		if (!request.isSecure() && !isDevServer()) {
			logger.warn("Insecure auth, Deny: " + request);
			throw new WebApplicationException(getForbiddenResponse(request, "Request insecure"));
		}

		if (userService.isUserLoggedIn()) {
			if (roles.contains(Role.ADMIN) && userService.isUserAdmin()) {
				logger.info("GAE admin auth, OK: " + request);
				setGaeSecurityContext(request, Role.ADMIN);
			} else {
				logger.info("GAE regular auth, OK: " + request);
				setGaeSecurityContext(request, Role.GAE);
			}
			return request;
		}

		if (roles.contains(Role.OAUTH)) {
			logger.trace("Authenticating OAuth user...");

			UserPrincipal user = verifyOauthToken(request);
			request.setSecurityContext(new OauthSecurityContext(user, true));
			return request;
		}

		logger.error("No known roles specified for endpoint {}", request.getPath());
		throw new WebApplicationException(getForbiddenResponse(request, "Forbidden"));
	}

	private void setGaeSecurityContext(ContainerRequest request, String role) {
		User user = userService.getCurrentUser();
		UserPrincipal userPrincipal = new UserPrincipal(user.getUserId(), user.getEmail());
		Set<String> roles = ImmutableSet.of(role, Role.GAE, Role.AUTHENTICATED);

		request.setSecurityContext(
				new GaeSecurityContext(userPrincipal, request.isSecure(), roles));
	}

	private UserPrincipal verifyOauthToken(ContainerRequest request) {
		Map<String,Cookie> cookies = request.getCookies();
		Cookie cookie = cookies.get(COOKIE_NAME_AUTH_TOKEN);
		if (cookie == null) {
			logger.warn("No authToken auth, Deny: " + request);
			throw new WebApplicationException(
					getForbiddenResponse(request, "No Cookie '" + COOKIE_NAME_AUTH_TOKEN + "'"));
		}

		try {
			return tokenVerifier.verify(cookie.getValue());
		} catch (GeneralSecurityException e) {
			logger.info("Invalid token", e);
			throw new WebApplicationException(e, getForbiddenResponse(request, "Invalid token"));
		} catch (IOException e) {
			logger.error("IOException verifying OAuth token", e);
			throw new WebApplicationException(e, getForbiddenResponse(request, "Error verifying token"));
		}
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
		return roles.isEmpty() ? "FORBIDDEN" : roles.toString();
	}
}
