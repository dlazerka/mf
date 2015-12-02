package me.lazerka.mf.gae.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.utils.SystemProperty;
import com.google.appengine.api.utils.SystemProperty.Environment.Value;
import com.google.common.collect.ImmutableSet;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;
import me.lazerka.mf.api.ApiConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.appengine.api.utils.SystemProperty.Environment.Value.Development;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * @see <a href="https://developers.google.com/identity/sign-in/android/backend-auth">documentation</a>.
 * @author Dzmitry Lazerka
 */
public class AuthFilter implements ResourceFilter, ContainerRequestFilter {
	private static final Logger logger = LoggerFactory.getLogger(AuthFilter.class);

	@Inject
	UserService userService;

	@Inject
	URLFetchService urlFetchService;

	@Inject
	ObjectMapper objectMapper;

	private final GoogleIdTokenVerifier tokenVerifier;
	private final Set<String> audience;
	private final Set<String> allowedIss = ImmutableSet.of("accounts.google.com", "https://accounts.google.com");

	@Inject
	public AuthFilter(@Named("oauth.client.id") String oauthClientId, UrlFetchTransport transport) {
		audience = ImmutableSet.of(checkNotNull(oauthClientId));

		tokenVerifier = new GoogleIdTokenVerifier.Builder(transport, JacksonFactory.getDefaultInstance())
				.setAudience(audience)
				.build();
	}

	private Set<String> roles;

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

		// Allow HTTP, on local dev server only.
		if (roles.contains(Role.DEVSERVER) && isDevServer()) {
			logger.info("Dev auth, OK: " + request);
			return request;
		}

		// Deny all HTTP requests (@PermitAll requests do not come here at all).
		if (!request.isSecure() && !isDevServer()) {
			logger.warn("Insecure auth, Deny: " + request);
			throw getForbiddenException(request, "Request insecure");
		}

		if (roles.contains(Role.ADMIN) && userService.isUserLoggedIn() && userService.isUserAdmin()) {
			logger.info("Admin auth, OK: " + request);
			return request;
		}

		if (roles.contains(Role.OAUTH)) {
			logger.trace("Authenticating OAuth user...");
			GoogleIdToken token = verifyCookie(request);
			addUserContextToRequest(request, token);
			return request;
		}

		logger.error("No known roles specified for endpoint {}", request.getPath());
		throw getForbiddenException(request, "Forbidden");
	}

	private GoogleIdToken verifyCookie(ContainerRequest request) {
		Map<String,Cookie> cookies = request.getCookies();
		Cookie cookie = cookies.get(ApiConstants.COOKIE_NAME_AUTH_TOKEN);
		if (cookie == null) {
			logger.warn("No authToken auth, Deny: " + request);
			throw getForbiddenException(request, "No Auth Token");
		}
		String authToken = cookie.getValue();
		try {
			//return verifyHttp(authToken, request);
			return verify(authToken, request);
		} catch (MalformedURLException e) {
			logger.error("Hacker's sent malformed token: {}", authToken, e);
		} catch (IOException e) {
			logger.error("Error fetching url", e);
		}

		throw getForbiddenException(request, "Error verifying token");
	}

	private GoogleIdToken verify(String authToken, ContainerRequest request) throws IOException {
		GoogleIdToken token;

		try {
			token = tokenVerifier.verify(authToken);
		} catch (GeneralSecurityException e) {
			logger.warn("Excepting while verifying token", e);
			throw getForbiddenException(request, "Excepting while verifying token");
		}

		if (token == null) {
			logger.info("Token invalid");
			throw getForbiddenException(request, "Token invalid");
		}

		return token;
	}

	private void addUserContextToRequest(ContainerRequest request, GoogleIdToken token) {
		request.setSecurityContext(new MfSecurityContext(token));
	}

	/**
	 * Example verification using Google's webserver.
	 * It's perfectly secure to do this in production, but adds additional latency.
	 */
	private GoogleIdToken verifyHttp(String authToken, ContainerRequest request) throws IOException {
		//URL url = new URL("https://www.googleapis.com/auth/userinfo.email?access_token=" + authToken);
		//URL url = new URL("https://www.googleapis.com/oauth2/v3/userinfo?alt=json&access_token=" + authToken);

		URL url = new URL("https://www.googleapis.com/oauth2/v3/tokeninfo?id_token=" + authToken);

		HTTPResponse response = urlFetchService.fetch(url);
		int responseCode = response.getResponseCode();
		String content = new String(response.getContent(), StandardCharsets.UTF_8);
		logger.debug("{}: {}", responseCode, content);

		if (responseCode != 200) {
			throw getForbiddenException(request, content);
		}

		JsonNode tree = objectMapper.readTree(content);
		checkState(!tree.hasNonNull("error"));

		verifyThat(allowedIss.contains(tree.get("iss").asText()), request, content);
		verifyThat(audience.contains(tree.get("aud").asText()), request, content);
		// Other fields were validated by remote endpoint.
		// TODO test it

		return GoogleIdToken.parse(tokenVerifier.getJsonFactory(), authToken);
	}

	private void verifyThat(boolean passed, ContainerRequest request, String content) {
		if (!passed) {
			throw getForbiddenException(request, content);
		}
	}

	private WebApplicationException getForbiddenException(ContainerRequest request, String reason) {
		List<String> loginReturnUrls = request.getRequestHeader("X-Login-Return-Url");
		String loginReturnUrl;
		if (loginReturnUrls == null || loginReturnUrls.isEmpty()) {
			loginReturnUrl = request.getRequestUri().toASCIIString();
		} else {
			loginReturnUrl = URI.create(loginReturnUrls.get(0)).toASCIIString();
		}
		String url = userService.createLoginURL(loginReturnUrl);

		Response response = Response
				.status(Status.FORBIDDEN)
				.type(MediaType.APPLICATION_JSON_TYPE)
				.entity(reason)
				.header("X-Login-URL", url)
				.build();
		return new WebApplicationException(response);
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
