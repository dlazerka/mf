/*
 *     Copyright (C) 2017 Dzmitry Lazerka
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package me.lazerka.mf.android.background.gcm;

import android.app.IntentService;
import android.content.Intent;
import android.content.pm.PackageInfo;
import com.baraded.mf.logging.LogService;
import com.baraded.mf.logging.Logger;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.iid.FirebaseInstanceId;
import me.lazerka.mf.android.auth.GoogleSignInException;
import me.lazerka.mf.android.auth.SignInManager;
import me.lazerka.mf.android.background.RequestFactory;
import me.lazerka.mf.android.di.Injector;
import me.lazerka.mf.api.object.GcmToken;
import okhttp3.Call;
import okhttp3.Response;

import javax.inject.Inject;
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
	private static final Logger log = LogService.getLogger(SendTokenToServerService.class);

	@Inject
	PackageInfo packageInfo;

	@Inject
	RequestFactory requestFactory;

	public SendTokenToServerService() {
		super(SendTokenToServerService.class.getSimpleName());
		Injector.applicationComponent().inject(this);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		try {
			GoogleSignInAccount signInAccount = new SignInManager().getAccountBlocking(this);

			String gcmToken = FirebaseInstanceId.getInstance().getToken();

			if (gcmToken == null) {
				log.warn("token is null");
				return;
			}

			GcmToken content = new GcmToken(gcmToken, packageInfo.versionCode);
			Call call = requestFactory.newPost(content);
			Response response = call.execute();

			if (response.code() != HttpURLConnection.HTTP_OK) {
				log.error("Unsuccessful sending GCM token: {} {}", response.code(), response.message());
			}

		} catch (GoogleSignInException e) {
			if (e.getStatus().getStatusCode() == SIGN_IN_REQUIRED) {
				// Always happens on the very first start, when user is still selecting account.
				log.warn("Not signed in, not sending token to server.");
			} else {
				log.error(e);
			}
		} catch (IOException e) {
			log.error(e);
		}
	}
}
