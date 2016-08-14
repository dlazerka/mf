/*
 *     Find Us: privacy oriented location tracker for your friends and family.
 *     Copyright (C) 2016 Dzmitry Lazerka dlazerka@gmail.com
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

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.iid.FirebaseInstanceId;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Response;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.auth.GoogleSignInException;
import me.lazerka.mf.android.auth.SignInManager;
import me.lazerka.mf.android.background.ApiPost;
import me.lazerka.mf.api.object.GcmToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;

import static com.google.android.gms.common.api.CommonStatusCodes.SIGN_IN_REQUIRED;

/**
 * Just sends the FCM token to server.
 *
 * Doesn't do it in {@link InstanceIdService}, because on the very first app start, there's no account signed in yet,
 * so we cannot authenticate to server. But after we authenticate, we cannot relaunch {@link InstanceIdService},
 * so we need a separate service.
 * @author Dzmitry Lazerka
 */
public class SendTokenToServerService extends IntentService {
	private static final Logger logger = LoggerFactory.getLogger(SendTokenToServerService.class);

	public SendTokenToServerService() {
		super(SendTokenToServerService.class.getSimpleName());
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		try {
			GoogleSignInAccount signInAccount = new SignInManager().getAccountBlocking(this);

			String gcmToken = FirebaseInstanceId.getInstance().getToken();

			if (gcmToken == null) {
				FirebaseCrash.logcat(Log.WARN, logger.getName(), "token is null");
				return;
			}

			GcmToken content = new GcmToken(gcmToken, Application.getVersion());
			ApiPost apiPost = new ApiPost(content);
			Call call = apiPost.newCall(signInAccount);
			Response response = call.execute();

			if (response.code() != HttpURLConnection.HTTP_OK) {
				String msg = "Unsuccessful sending GCM token: " + response.code() + " " + response.message();
				FirebaseCrash.report(new IOException(msg));
			}

		} catch (GoogleSignInException e) {
			if (e.getStatus().getStatusCode() == SIGN_IN_REQUIRED) {
				// Always happens on the very first start, when user is still selecting account.
				FirebaseCrash.logcat(Log.INFO, logger.getName(), "Not signed in, not sending token to server.");
			} else {
				FirebaseCrash.report(e);
				logger.error(e.getMessage(), e);
			}
		} catch (IOException e) {
			FirebaseCrash.report(e);
			logger.error(e.getMessage(), e);
		}
	}
}
