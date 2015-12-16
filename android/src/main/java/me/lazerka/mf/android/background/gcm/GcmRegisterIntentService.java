package me.lazerka.mf.android.background.gcm;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.WakefulBroadcastReceiver;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Response;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.R;
import me.lazerka.mf.android.auth.SignInManager;
import me.lazerka.mf.android.background.ApiPost;
import me.lazerka.mf.api.object.GcmToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * Requests GCM token, and sends it to server.
 *
 * If an old token is still valid, Android will not create a new one. But we will send it again just in case.
 *
 * @author Dzmitry Lazerka
 */
public class GcmRegisterIntentService extends IntentService {
	private static final Logger logger = LoggerFactory.getLogger(GcmRegisterIntentService.class);

	public static final String GCM_REGISTRATION_COMPLETE = "GCM_REGISTRATION_COMPLETE";

	private static final String TAG = "RegIntentService";
	private static final String[] TOPICS = {"global"};

	public GcmRegisterIntentService() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		try {
			// Initially this call goes out to the network to retrieve the token, subsequent calls are local.
			// R.string.gcm_defaultSenderId (the Sender ID) is typically derived from google-services.json.
			InstanceID instanceID = InstanceID.getInstance(this);
			String senderId = getString(R.string.gcm_defaultSenderId);
			String token = instanceID.getToken(senderId, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);

			logger.info("GCM Token refreshed: {}, sending...");

			sendRegistrationToServer(token);

			// Subscribe to topic channels
			//subscribeTopics(token);

			// You should store a boolean that indicates whether the generated token has been
			// sent to your server. If the boolean is false, send the token to your server,
			// otherwise your server should have already received the token.
			Application.preferences.setGcmTokenSent(token);
		} catch (Exception e) {
			logger.warn("Failed to complete token refresh", e);
			// If an exception happens while fetching the new token or updating our registration data
			// on a third-party server, this ensures that we'll attempt the update at a later time.
			Application.preferences.clearGcmTokenSent();

			// TODO retry GCM registration
			// If there is an error, don't just keep trying to register.
			// Require the user to click a button again, or perform
			// exponential back-off.
		}

		// Notify UI that registration has completed, so the progress indicator can be hidden.
		Intent registrationComplete = new Intent(GCM_REGISTRATION_COMPLETE);
		LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);

		// Release the wake lock provided by the WakefulBroadcastReceiver, if was started with.
		WakefulBroadcastReceiver.completeWakefulIntent(intent);
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
			throw new IOException(msg);
		}
	}

	/**
	 * Subscribe to any GCM topics of interest, as defined by the TOPICS constant.
	 *
	 * @param token GCM token
	 * @throws IOException if unable to reach the GCM PubSub service
	 */
	private void subscribeTopics(String token) throws IOException {
		GcmPubSub pubSub = GcmPubSub.getInstance(this);
		for (String topic : TOPICS) {
			pubSub.subscribe(token, "/topics/" + topic, null);
		}
	}
}
