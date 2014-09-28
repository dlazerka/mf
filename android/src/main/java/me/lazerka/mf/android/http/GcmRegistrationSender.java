package me.lazerka.mf.android.http;

import android.util.Log;
import com.android.volley.Request.Method;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.api.gcm.GcmRegistrationResponse;
import me.lazerka.mf.api.object.GcmRegistration;

import javax.annotation.Nonnull;

/**
 * Sends GCM Registration ID (aka token) to our server, so that server has a hook to communicate back to this device.
 *
 * @author Dzmitry Lazerka
 */
public class GcmRegistrationSender extends JsonRequester<GcmRegistration, GcmRegistrationResponse> {
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
