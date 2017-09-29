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

/**
 * Requests GCM token, and sends it to server.
 *
 * If an old token is still valid, Android will not create a new one. But we will send it again just in case.
 *
 * @author Dzmitry Lazerka
 */
//public class GcmRegisterIntentService extends IntentService {
//	private static final Logger logger = LogService.getLogger(GcmRegisterIntentService.class);
//
//	public static final String GCM_REGISTRATION_COMPLETE = "GCM_REGISTRATION_COMPLETE";
//
//	private static final String TAG = "RegIntentService";
//	private static final String[] TOPICS = {"global"};
//
//	public GcmRegisterIntentService() {
//		super(TAG);
//	}
//
//	@Override
//	protected void onHandleIntent(Intent intent) {
//		try {
//			// Initially this call goes out to the network to retrieve the token, subsequent calls are local.
//			// R.string.gcm_defaultSenderId (the Sender ID) is typically derived from google-services.json.
//			InstanceID instanceID = InstanceID.getInstance(this);
//			String senderId = getString(R.string.gcm_defaultSenderId);
//			String token = instanceID.getToken(senderId, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
//
//			logger.info("GCM Token refreshed, sending...");
//
//			sendRegistrationToServer(token);
//
//			// Subscribe to topic channels
//			//subscribeTopics(token);
//
//			// You should store a boolean that indicates whether the generated token has been
//			// sent to your server. If the boolean is false, send the token to your server,
//			// otherwise your server should have already received the token.
//			Application.gcmManager.setGcmTokenSent(token);
//		} catch (Exception e) {
//			logger.warn("Failed to complete token refresh", e);
//			// If an exception happens while fetching the new token or updating our registration data
//			// on a third-party server, this ensures that we'll attempt the update at a later time.
//			Application.gcmManager.clearGcmTokenSent();
//
//			// TODO retry GCM registration
//			// If there is an error, don't just keep trying to register.
//			// Require the user to click a button again, or perform
//			// exponential back-off.
//		}
//
//		// Notify UI that registration has completed, so the progress indicator can be hidden.
//		Intent registrationComplete = new Intent(GCM_REGISTRATION_COMPLETE);
//		LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
//
//		// Release the wake lock provided by the WakefulBroadcastReceiver, if was started with.
//		WakefulBroadcastReceiver.completeWakefulIntent(intent);
//	}
//
//	/**
//	 * Make backend aware of the token.
//	 */
//	private void sendRegistrationToServer(String gcmToken) throws IOException {
//		GoogleSignInAccount signInAccount = new SignInManager()
//				.getAccountBlocking(this);
//
//		GcmToken content = new GcmToken(gcmToken, Application.getVersion());
//		Call call = requestFactory.newPost(content);
//		Response response = call.execute();
//
//		if (response.code() != HttpURLConnection.HTTP_OK) {
//			String msg = "Unsuccessful sending GCM token: " + response.code() + " " + response.message();
//			throw new IOException(msg);
//		}
//	}
//
//	/**
//	 * Subscribe to any GCM topics of interest, as defined by the TOPICS constant.
//	 *
//	 * @param token GCM token
//	 * @throws IOException if unable to reach the GCM PubSub service
//	 */
//	private void subscribeTopics(String token) throws IOException {
//		GcmPubSub pubSub = GcmPubSub.getInstance(this);
//		for (String topic : TOPICS) {
//			pubSub.subscribe(token, "/topics/" + topic, null);
//		}
//	}
//}
