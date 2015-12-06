package me.lazerka.mf.gae.oauth;

import com.google.api.client.auth.oauth2.TokenErrorResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.json.JsonFactory;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.google.appengine.api.urlfetch.FetchOptions.Builder.validateCertificate;
import static com.google.appengine.api.urlfetch.HTTPMethod.GET;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Filter that verifies token by making HTTPS call to Google endpoint, so Google servers verify it.
 *
 * This is pretty safe, as long as done through HTTPS, but adds latency.
 *
 * @see <a href="https://developers.google.com/identity/sign-in/android/backend-auth">documentation</a>.
 * @author Dzmitry Lazerka
 */
public class TokenVerifierRemote implements TokenVerifier {
	private static final Logger logger = LoggerFactory.getLogger(TokenVerifierRemote.class);

	private static final UriBuilder endpoint =
			UriBuilder.fromUri("https://www.googleapis.com/oauth2/v3/tokeninfo")
					.queryParam("id_token={token}");

	@Inject
	URLFetchService urlFetchService;

	@Inject
	JsonFactory jsonFactory;

	@Inject
	@Named(OauthModule.OAUTH_CLIENT_ID)
	String oauthClientId;

	@Override
	public UserPrincipal verify(String authToken) throws IOException, InvalidKeyException {
		logger.trace("Requesting endpoint to validate token");

		URL url = endpoint.build(authToken).toURL();

		HTTPRequest httpRequest = new HTTPRequest(url, GET, validateCertificate());

		Stopwatch stopwatch = Stopwatch.createStarted();
		HTTPResponse response = urlFetchService.fetch(httpRequest);
		logger.debug("Remote call took {}ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));

		int responseCode = response.getResponseCode();
		String content = new String(response.getContent(), UTF_8);

		if (responseCode != 200) {
			logger.warn("{}: {}", responseCode, content);

			String msg = "Endpoint response code " + responseCode;

			// Something is wrong with our request.
			// If signature is invalid, then response code is 403.
			if (responseCode >= 400 && responseCode < 500) {
				try {
					TokenErrorResponse tokenErrorResponse = jsonFactory.fromString(content, TokenErrorResponse.class);
					msg += ": " + tokenErrorResponse.getErrorDescription();
				} catch (IOException e) {
					logger.warn("Cannot parse response as " + TokenErrorResponse.class.getSimpleName());
				}
			}

			throw new InvalidKeyException(msg);
		}

		// Signature verification is done remotely (the whole point of this class).
		// Expiration verification is done

		Payload payload = jsonFactory.fromString(content, Payload.class);

		// Issuers verification should have been done remotely, but we do it again.
		if (!OauthModule.ALLOWED_ISSUERS.contains(payload.getIssuer())) {
			throw new InvalidKeyException("Issuer invalid");
		}

		Set<String> trustedClientIds = Collections.singleton(oauthClientId);
		// Note containsAll.
		if (!trustedClientIds.containsAll(payload.getAudienceAsList())) {
			throw new InvalidKeyException("Audience invalid");
		}

		if (!payload.getEmailVerified()) {
			throw new InvalidKeyException("Email not verified");
		}

		return new UserPrincipal(payload.getSubject(), payload.getEmail());
	}
}