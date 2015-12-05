package me.lazerka.mf.gae.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
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

import static com.google.appengine.api.urlfetch.FetchOptions.Builder.validateCertificate;
import static com.google.appengine.api.urlfetch.HTTPMethod.GET;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Filter that verifies token by making HTTPS call to remote endpoint.
 *
 * @see <a href="https://developers.google.com/identity/sign-in/android/backend-auth">documentation</a>.
 * @author Dzmitry Lazerka
 */
public class AuthFilterRemoteVerify extends AuthFilter {
	private static final Logger logger = LoggerFactory.getLogger(AuthFilterRemoteVerify.class);

	private static final UriBuilder endpoint =
			UriBuilder.fromUri("https://www.googleapis.com/oauth2/v3/tokeninfo?id_token={token}");

			//UriBuilder.fromUri("https://www.googleapis.com/oauth2/v3/userinfo?alt=json&access_token={token}");
			//UriBuilder.fromUri("https://www.googleapis.com/auth/userinfo.email?access_token={token}");

	@Inject
	URLFetchService urlFetchService;

	@Inject
	ObjectMapper objectMapper;

	@Inject
	@Named(OauthModule.OAUTH_CLIENT_ID)
	String oauthClientId;

	@Override
	protected OauthUser verify(String authToken) throws IOException, InvalidKeyException {
		logger.trace("Requesting endpoint to validate token");

		URL url = endpoint.build(authToken).toURL();

		HTTPRequest httpRequest = new HTTPRequest(url, GET, validateCertificate());

		HTTPResponse response = urlFetchService.fetch(httpRequest);

		int responseCode = response.getResponseCode();
		String content = new String(response.getContent(), UTF_8);

		if (responseCode != 200) {
			logger.warn("{}: {}", responseCode, content);
			throw new InvalidKeyException("Endpoint response code " + responseCode);
		}

		// Signature verification is done
		// Issuers verification is done
		// Expiration verification is done

		Payload payload = objectMapper.readValue(content, Payload.class);

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

		return new OauthUser(payload.getSubject(), payload.getEmail());
	}

}
