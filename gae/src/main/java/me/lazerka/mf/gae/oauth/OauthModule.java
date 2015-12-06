package me.lazerka.mf.gae.oauth;

import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier.Builder;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.name.Names;
import me.lazerka.mf.gae.MainModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.util.Set;

/**
 * Configuration needed for OAuth web authentication.
 *
 * @author Dzmitry Lazerka
 */
public class OauthModule extends AbstractModule {
	private static final Logger logger = LoggerFactory.getLogger(OauthModule.class);

	public static final String OAUTH_CLIENT_ID = "oauth.client.id";
	public static final Set<String> ALLOWED_ISSUERS = ImmutableSet.of(
			"accounts.google.com",
			"https://accounts.google.com");

	@Override
	protected void configure() {
		// Read config files early, so that errors would pop up on startup.
		bind(Key.get(String.class, Names.named(OAUTH_CLIENT_ID)))
				.toInstance(readOauthClientId());

		// Choose between verification methods.
		bind(TokenVerifier.class).to(TokenVerifierSignature.class);
		//bind(AuthFilter.class).to(TokenVerifierRemote.class);
	}

	@Inject
	@Provides
	@Singleton
	GoogleIdTokenVerifier createTokenVerifier(
			@Named(OAUTH_CLIENT_ID) String oauthClientId,
			JsonFactory jsonFactory
	) {
		logger.trace("Creating " + GoogleIdTokenVerifier.class.getSimpleName());
		UrlFetchTransport transport = new UrlFetchTransport.Builder()
				.validateCertificate()
				.build();
		return new Builder(transport, jsonFactory)
				.setAudience(ImmutableSet.of(oauthClientId))
				.setIssuers(ALLOWED_ISSUERS)
				.build();
	}

	@Provides
	@Singleton
	JsonFactory getJsonFactory() {
		return JacksonFactory.getDefaultInstance();
	}

	String readOauthClientId() {
		File file = new File("WEB-INF/secret/oauth.client_id.key");
		String notFoundMsg = "Put there OAuth2.0 Client ID obtained as described here " +
				"https://developers.google.com/identity/sign-in/android/";

		String result = MainModule.readFileString(file, notFoundMsg);

		if (!result.endsWith(".apps.googleusercontent.com")) {
			throw new RuntimeException("Must end with '.apps.googleusercontent.com'");
		}

		return result;
	}
}