package me.lazerka.mf.gae.oauth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;

import javax.inject.Inject;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;

/**
 * Filter that verifies OAuth token using public key signature check.
 *
 * Utilizes google-api-client for that.
 *
 * @see <a href="https://developers.google.com/identity/sign-in/android/backend-auth">documentation</a>.
 * @author Dzmitry Lazerka
 */
public class TokenVerifierSignature implements TokenVerifier {
	@Inject
	GoogleIdTokenVerifier tokenVerifier;

	@Override
	public OauthUser verify(String token) throws IOException, GeneralSecurityException {
		GoogleIdToken idToken = GoogleIdToken.parse(tokenVerifier.getJsonFactory(), token);

		if (!tokenVerifier.verify(idToken)) {
			String email = idToken.getPayload().getEmail();
			throw new InvalidKeyException("Invalid token for allegedly " + email);
		}

		Payload payload = idToken.getPayload();
		return new OauthUser(payload.getSubject(), payload.getEmail());
	}
}
