package me.lazerka.mf.android.auth;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;
import com.android.volley.Request.Method;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.http.JsonRequester;
import me.lazerka.mf.api.gcm.GcmRegistrationResponse;
import me.lazerka.mf.api.object.GcmRegistration;
import org.acra.ACRA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * Deals with GCM registration -- obtains GCM registration ID (aka GCM-token), and stores it.
 *
 * @author Dzmitry Lazerka
 */
public class GcmAuthenticator {
	private static final Logger logger = LoggerFactory.getLogger(GcmAuthenticator.class);

	/**
	 * Project number obtained from the API Console, as described in GCM "Getting Started".
	 */
	private static final String SENDER_ID = "769083712074";
	private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

	private GoogleCloudMessaging gcm;
	private String gcmToken;
	private final Context context;

	public GcmAuthenticator(Context context) {
		this.context = context;
	}

	/**
	 * May be called from both foreground and background threads.
	 *
	 * @param gcmToken to store.
	 */
	public static void storeGcmRegistration(String gcmToken) {
		// Persist the regID - no need to register again.
		Application.preferences.setGcmToken(gcmToken);

		// You should send the registration ID to your server over HTTP,
		// so it can use GCM/HTTP or CCS to send messages to your app.
		new GcmRegistrationSender(gcmToken)
				.send();
	}


	public void checkRegistration() {
		if (checkPlayServices()) {
			gcm = GoogleCloudMessaging.getInstance(context);
			gcmToken = Application.preferences.getGcmToken();

			if (gcmToken == null) {
				new GcmRegisterTask().execute();
			} else {
				// Unconditionally send gcmToken, because server might have removed it, even if we sent it before.
				logger.info("Sending GCM token to server");
				new GcmRegistrationSender(gcmToken)
						.send();
			}
		} else {
			logger.error("No valid Google Play Services APK found.");
			Toast.makeText(context, "Please install Google Play Services", Toast.LENGTH_LONG)
					.show();
		}
	}

	/**
	 * https://developer.android.com/google/gcm/client.html
	 * Check the device to make sure it has the Google Play Services APK. If
	 * it doesn't, display a dialog that allows users to download the APK from
	 * the Google Play Store or enable it in the device's system settings.
	 */
	private boolean checkPlayServices() {
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
		if (resultCode != ConnectionResult.SUCCESS) {
			logger.warn("Google Play Services unavailable: " + resultCode);
			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
				if (context instanceof Activity) {
					GooglePlayServicesUtil.getErrorDialog(
							resultCode,
							(Activity) context,
							PLAY_SERVICES_RESOLUTION_REQUEST
					)
					.show();
				} else {
					String msg = "Play Services unavailable, but cannot show error dialog to user.";
					logger.error(msg);
					ACRA.getErrorReporter().handleException(new Exception(msg), true);
				}
			} else {
				logger.info("This device is not supported.");
			}
			return false;
		}
		return true;
	}


	/**
	 * Registers the application with GCM servers asynchronously.
	 * <p>
	 * Stores the registration ID and app versionCode in the application's
	 * shared preferences.
	 */
	private class GcmRegisterTask extends AsyncTask<Void, Void, String> {
		@Override
		protected String doInBackground(Void... params) {
			try {
				if (gcm == null) {
					gcm = GoogleCloudMessaging.getInstance(Application.context);
				}
				logger.info("Calling GCM.register()");
				gcmToken = gcm.register(SENDER_ID);
				logger.info("GCM.register() successful");

				storeGcmRegistration(gcmToken);

				return "Device registered";
			} catch (IOException e) {
				// On Sony Xperia happens all the time, but fortunately GcmBroadcastReceiver receives the regId.
				logger.info("GCM.register() failed: " + e.getMessage());
				return "Error: " + e.getMessage();
				// If there is an error, don't just keep trying to register.
				// Require the user to click a button again, or perform
				// exponential back-off.
			}
		}

		@Override
		protected void onPostExecute(String msg) {
			if (msg.contains("Error")) {
				logger.warn(msg);
				Toast.makeText(context, "Google Cloud Messaging " + msg, Toast.LENGTH_LONG)
						.show();
			} else {
				logger.info(msg);
			}
		}
	}

	/**
	 * Sends GCM Registration ID (aka token) to our server, so that server has a hook to communicate back to this device.
	 *
	 * @author Dzmitry Lazerka
	 */
	private static class GcmRegistrationSender extends JsonRequester<GcmRegistration, GcmRegistrationResponse> {
		public GcmRegistrationSender(@Nonnull String gcmRegistrationId) {
			super(
					Method.POST,
					GcmRegistration.PATH,
					new GcmRegistration(gcmRegistrationId, Application.getVersion()),
					GcmRegistrationResponse.class);
		}

		@Override
		public void onResponse(GcmRegistrationResponse response) {
			GcmRegistration request = getRequest();
			logger.info("Server stored our registration ID: " + request.getId());
		}
	}
}
