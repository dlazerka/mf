package me.lazerka.mf.android.auth;

import android.app.Activity;
import android.content.Context;
import com.android.volley.Request.Method;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.http.JsonRequester;
import me.lazerka.mf.api.gcm.GcmRegistrationResponse;
import me.lazerka.mf.api.object.GcmRegistration;
import org.acra.ACRA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

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
	private final Context context;

	public GcmAuthenticator(Context context) {
		this.context = context;
	}


	/**
	 * https://developer.android.com/google/gcm/client.html
	 * Check the device to make sure it has the Google Play Services APK. If
	 * it doesn't, display a dialog that allows users to download the APK from
	 * the Google Play Store or enable it in the device's system settings.
	 */
	private boolean checkPlayServices() {
		GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
		int resultCode = apiAvailability.isGooglePlayServicesAvailable(context);
		if (resultCode == ConnectionResult.SUCCESS) {
			return true;
		}

		logger.warn("Google Play Services unavailable: " + resultCode);

		if (apiAvailability.isUserResolvableError(resultCode)) {
			if (context instanceof Activity) {
				logger.info("Showing ErrorDialog to user...");
				apiAvailability.getErrorDialog((Activity) context, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
						.show();
			} else {
				String msg = "Play Services unavailable, but cannot show error dialog to user.";
				logger.error(msg);
				ACRA.getErrorReporter().handleException(new Exception(msg), true);
			}
		} else {
			String msg = "This device doesn't support Play Services.";
			logger.error(msg);
			ACRA.getErrorReporter().handleException(new Exception(msg), true);
		}

		return false;
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
