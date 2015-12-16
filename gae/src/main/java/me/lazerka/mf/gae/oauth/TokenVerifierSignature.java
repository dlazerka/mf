package me.lazerka.mf.gae.oauth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.joda.time.DateTime;

import javax.inject.Inject;
import javax.inject.Named;
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

	@Inject
	@Named("now")
	DateTime now;

	@Override
	public UserPrincipal verify(String token) throws IOException, GeneralSecurityException {
		GoogleIdToken idToken = GoogleIdToken.parse(tokenVerifier.getJsonFactory(), token);

		if (!tokenVerifier.verify(idToken)) {
			String email = idToken.getPayload().getEmail();

			// Give meaningful message for the most common case.
			if (!idToken.verifyTime(now.getMillis(), tokenVerifier.getAcceptableTimeSkewSeconds())) {
				throw new InvalidKeyException("Token expired for allegedly " + email);
			}

			throw new InvalidKeyException("Invalid token for allegedly " + email);
		}

		Payload payload = idToken.getPayload();
		return new UserPrincipal(payload.getSubject(), payload.getEmail());
	}
}
