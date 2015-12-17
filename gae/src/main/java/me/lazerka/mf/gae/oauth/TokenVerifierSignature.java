/*
 *     Find Us: privacy oriented location tracker for your friends and family.
 *     Copyright (C) 2015 Dzmitry Lazerka dlazerka@gmail.com
 *
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

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
