package me.lazerka.mf.android.auth;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import com.android.volley.Request.Method;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.http.JsonRequester;
import me.lazerka.mf.api.gcm.GcmRegistrationResponse;
import me.lazerka.mf.api.object.GcmRegistration;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * Deals with GCM registration -- obtains GCM registration ID (aka GCM-token), and stores it.
 *
 * @author Dzmitry Lazerka
 */
public class GcmAuthenticator {
	private static final String TAG = GcmAuthenticator.class.getName();

	/**
	 * Project number obtained from the API Console, as described in GCM "Getting Started".
	 */
	private static final String SENDER_ID = "769083712074";
	private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

	private GoogleCloudMessaging gcm;
	private String gcmToken;
	private final Activity activity;

	public GcmAuthenticator(Activity activity) {
		this.activity = activity;
	}

	/**
	 * May be called from both foreground and background threads.
	 *
	 * @param gcmToken to store.
	 */
	public static void storeGcmRegistration(String gcmToken) {
		if (Application.preferences.getGcmServerKnowsToken(gcmToken)) {
			return;
		}

		// Persist the regID - no need to register again.
		Application.preferences.setGcmToken(gcmToken);

		// You should send the registration ID to your server over HTTP,
		// so it can use GCM/HTTP or CCS to send messages to your app.
		new GcmRegistrationSender(gcmToken)
				.send();
	}


	public void checkRegistration() {
		if (checkPlayServices()) {
			gcm = GoogleCloudMessaging.getInstance(activity);
			gcmToken = Application.preferences.getGcmToken();

			if (gcmToken == null) {
				new GcmRegisterTask().execute();
			} else if (!Application.preferences.getGcmServerKnowsToken(gcmToken)) {
				new GcmRegistrationSender(gcmToken)
						.send();
			}
		} else {
			Log.e(TAG, "No valid Google Play Services APK found.");
			Toast.makeText(activity, "Please install Google Play Services", Toast.LENGTH_LONG)
					.show();

			activity.finish();
		}
	}

	/**
	 * https://developer.android.com/google/gcm/client.html
	 * Check the device to make sure it has the Google Play Services APK. If
	 * it doesn't, display a dialog that allows users to download the APK from
	 * the Google Play Store or enable it in the device's system settings.
	 */
	public boolean checkPlayServices() {
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
		if (resultCode != ConnectionResult.SUCCESS) {
			Log.w(TAG, "Google Play Services unavailable: " + resultCode);
			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
				GooglePlayServicesUtil.getErrorDialog(resultCode, activity, PLAY_SERVICES_RESOLUTION_REQUEST)
						.show();
			} else {
				Log.i(TAG, "This device is not supported.");
				activity.finish();
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
				Log.i(TAG, "Calling GCM.register()");
				gcmToken = gcm.register(SENDER_ID);
				Log.i(TAG, "GCM.register() successful");

				storeGcmRegistration(gcmToken);

				return "Device registered";
			} catch (IOException e) {
				Log.i(TAG, "GCM.register() failed: " + e.getMessage());
				return "Error: " + e.getMessage();
				// If there is an error, don't just keep trying to register.
				// Require the user to click a button again, or perform
				// exponential back-off.
			}
		}

		@Override
		protected void onPostExecute(String msg) {
			if (msg.contains("Error")) {
				Log.w(TAG, msg);
				Toast.makeText(activity, "Google Cloud Messaging " + msg, Toast.LENGTH_LONG)
						.show();
			} else {
				Log.i(TAG, msg);
			}
		}
	}

	/**
	 * Sends GCM Registration ID (aka token) to our server, so that server has a hook to communicate back to this device.
	 *
	 * @author Dzmitry Lazerka
	 */
	public static class GcmRegistrationSender extends JsonRequester<GcmRegistration, GcmRegistrationResponse> {
		private final String TAG = getClass().getName();

		public GcmRegistrationSender(@Nonnull String gcmRegistrationId) {
			super(
					Method.POST,
					GcmRegistration.PATH,
					new GcmRegistration(gcmRegistrationId, Application.getVersion()),
					GcmRegistrationResponse.class);
		}

		@Override
		public void onResponse(GcmRegistrationResponse response) {
			Log.i(TAG, "Server stored our registration ID as " + response.getId());
			GcmRegistration request = getRequest();
			Application.preferences.setGcmServerKnowsToken(request.getToken());
		}
	}
}