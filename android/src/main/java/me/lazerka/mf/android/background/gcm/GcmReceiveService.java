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

import android.support.annotation.WorkerThread;
import com.baraded.mf.Sw;
import com.baraded.mf.logging.LogService;
import com.baraded.mf.logging.Logger;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.api.gcm.GcmPayload;
import me.lazerka.mf.api.object.LocationRequestFromServer;
import me.lazerka.mf.api.object.LocationResponse;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.Map;

/**
 * Handler for incoming messages from Firebase.
 *
 * Endpoint is: fcm.googleapis.com/fcm/
 *
 * If sending a notification message and app is in background, notification is shown.
 * If sending a notification message and app is in foreground, onMessageReceived is called.
 *
 * @author Dzmitry Lazerka
 */
public class GcmReceiveService extends FirebaseMessagingService {
	private static final Logger log = LogService.getLogger(GcmReceiveService.class);

	@WorkerThread
	@Override
	public void onMessageReceived(RemoteMessage message) {
		Map<String, String> data = message.getData();
		String type = data.get(GcmPayload.TYPE_FIELD);
		String json = data.get(GcmPayload.PAYLOAD_FIELD);

		String from = message.getFrom();
		log.info("Received message from {}: {}", from, type);

		if (type == null) {
			log.warn("No {} field: {}", GcmPayload.PAYLOAD_FIELD, data);
			return;
		}

		if (json == null) {
			log.warn("No {} field", GcmPayload.PAYLOAD_FIELD);
			return;
		}

		try {
			Sw sw = Sw.realtime();
			switch (type) {
				case LocationResponse.TYPE:
					LocationResponse payload = Application.getJsonMapper().readValue(json, LocationResponse.class);
					log.info("Parsed LocationResponse in {}ms", sw.ms());
					Application.getLocationService()
							.handleLocationResponse(payload, from);
					break;
				case LocationRequestFromServer.TYPE:
					LocationRequestFromServer locationRequest =
							Application.getJsonMapper().readValue(json, LocationRequestFromServer.class);
					log.info("Parsed LocationResponse in {}ms", sw.ms());

					Application.getLocationService()
							.handleRequest(
									locationRequest,
									from,
									new DateTime(message.getSentTime())
							);
					break;
				default:
					log.error("Unknown message type: " + type);
			}
		} catch (IOException e) {
			log.warn("Cannot parse {}: {}", type, json, e);
			// Don't call onError(), because well-behaved observables can issue onError only once.
			// Observers probably cannot even do much with it anyway.
			// observer.onError(e);
		}

	}
}
