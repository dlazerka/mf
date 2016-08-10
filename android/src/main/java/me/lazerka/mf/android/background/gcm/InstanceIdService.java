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

package me.lazerka.mf.android.background.gcm;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Response;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.auth.SignInManager;
import me.lazerka.mf.android.background.ApiPost;
import me.lazerka.mf.api.object.GcmToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * @author Dzmitry Lazerka
 */
public class InstanceIdService extends FirebaseInstanceIdService {
	private static final Logger logger = LoggerFactory.getLogger(InstanceIdService.class);

	/**
	 * Called if InstanceID token is updated. This may occur if the security of
	 * the previous token had been compromised. Note that this is also called
	 * when the InstanceID token is initially generated, so this is where
	 * you retrieve the token.
	 */
	@Override
	public void onTokenRefresh() {
		// Fetch updated Instance ID token and notify our app's server of any changes (if applicable).
		//Intent intent = new Intent(this, GcmRegisterIntentService.class);
		//startService(intent);

		String refreshedToken = FirebaseInstanceId.getInstance().getToken();
		//logger.debug("Refreshed token: " + refreshedToken);
		try {
			sendRegistrationToServer(refreshedToken);
		} catch (IOException e) {
			FirebaseCrash.report(e);
			logger.warn("Cannot send registration ID to server", e);
		}
	}

	/**
	 * Make backend aware of the token.
	 */
	private void sendRegistrationToServer(String gcmToken) throws IOException {
		GoogleSignInAccount signInAccount = new SignInManager()
				.getAccountBlocking(this);

		GcmToken content = new GcmToken(gcmToken, Application.getVersion());
		ApiPost apiPost = new ApiPost(content);
		Call call = apiPost.newCall(signInAccount);
		Response response = call.execute();

		if (response.code() != HttpURLConnection.HTTP_OK) {
			String msg = "Unsuccessful sending GCM token: " + response.code() + " " + response.message();
			FirebaseCrash.report(new IOException(msg));
		}
	}

}
