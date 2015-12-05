package me.lazerka.mf.gae.oauth;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.utils.SystemProperty;
import com.google.appengine.api.utils.SystemProperty.Environment.Value;
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
 * @see <a href="https://developers.google.com/identity/sign-in/android/backend-auth">documentation</a>.
 * @author Dzmitry Lazerka
 */
abstract class AuthFilter implements ResourceFilter, ContainerRequestFilter {
	private static final Logger logger = LoggerFactory.getLogger(AuthFilter.class);

	@Inject
	UserService userService;

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

		// Allow HTTP, on local dev server only.
		if (roles.contains(Role.DEVSERVER) && isDevServer()) {
			logger.info("Dev auth, OK: " + request);
			return request;
		}

		// Deny all HTTP requests (@PermitAll requests do not come here at all).
		if (!request.isSecure() && !isDevServer()) {
			logger.warn("Insecure auth, Deny: " + request);
			throw new WebApplicationException(getForbiddenResponse(request, "Request insecure"));
		}

		if (roles.contains(Role.ADMIN) && userService.isUserLoggedIn() && userService.isUserAdmin()) {
			logger.info("Admin auth, OK: " + request);
			return request;
		}

		if (roles.contains(Role.OAUTH)) {
			logger.trace("Authenticating OAuth user...");

			OauthUser user = verifyOauthToken(request);
			request.setSecurityContext(new OauthSecurityContext(user, true));
			return request;
		}

		logger.error("No known roles specified for endpoint {}", request.getPath());
		throw new WebApplicationException(getForbiddenResponse(request, "Forbidden"));
	}

	private OauthUser verifyOauthToken(ContainerRequest request) {
		Map<String,Cookie> cookies = request.getCookies();
		Cookie cookie = cookies.get(COOKIE_NAME_AUTH_TOKEN);
		if (cookie == null) {
			logger.warn("No authToken auth, Deny: " + request);
			throw new WebApplicationException(
					getForbiddenResponse(request, "No Auth Cookie " + COOKIE_NAME_AUTH_TOKEN));
		}

		try {
			return verify(cookie.getValue());
		} catch (GeneralSecurityException e) {
			logger.warn("Invalid token", e);
			throw new WebApplicationException(getForbiddenResponse(request, "Invalid token"));
		} catch (IOException e) {
			logger.error("IOException verifying OAuth token", e);
			throw new WebApplicationException(getForbiddenResponse(request, "Error verifying token"));
		}
	}

	protected abstract OauthUser verify(String authToken) throws IOException, GeneralSecurityException;

	private Response getForbiddenResponse(ContainerRequest request, String reason) {
		String url = composeLoginUrl(request);
		return Response
				.status(Status.FORBIDDEN)
				.type(MediaType.TEXT_PLAIN_TYPE)
				.entity(reason)
				.header("X-Login-URL", url)
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
