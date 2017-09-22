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
import android.util.Log;
import com.google.common.base.Stopwatch;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import me.lazerka.mf.android.AndroidTicker;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.api.gcm.GcmPayload;
import me.lazerka.mf.api.object.LocationRequestFromServer;
import me.lazerka.mf.api.object.LocationResponse;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

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
	private static final Logger logger = LoggerFactory.getLogger(GcmReceiveService.class);

	@WorkerThread
	@Override
	public void onMessageReceived(RemoteMessage message) {
		Map<String, String> data = message.getData();
		String type = data.get(GcmPayload.TYPE_FIELD);
		String json = data.get(GcmPayload.PAYLOAD_FIELD);

		String from = message.getFrom();
		logger.info("Received message from {}: {}", from, type);

		if (type == null) {
			FirebaseCrash.logcat(Log.WARN, logger.getName(), format("No %s field: %s", GcmPayload.PAYLOAD_FIELD, data));
			return;
		}

		if (json == null) {
			logger.warn("No {} field", GcmPayload.PAYLOAD_FIELD);
			return;
		}

		try {
			Stopwatch stopwatch = AndroidTicker.started();
			switch (type) {
				case LocationResponse.TYPE:
					LocationResponse payload = Application.getJsonMapper().readValue(json, LocationResponse.class);
					logger.info("Parsed LocationResponse in {}ms", stopwatch.elapsed(MILLISECONDS));
					Application.getLocationService()
							.handleLocationResponse(payload, from);
					break;
				case LocationRequestFromServer.TYPE:
					LocationRequestFromServer locationRequest =
							Application.getJsonMapper().readValue(json, LocationRequestFromServer.class);
					logger.info("Parsed LocationResponse in {}ms", stopwatch.elapsed(MILLISECONDS));

					Application.getLocationService()
							.handleRequest(
									locationRequest,
									from,
									new DateTime(message.getSentTime())
							);
					break;
				default:
					FirebaseCrash.report(new Exception("Unknown message type: " + type));
			}
		} catch (IOException e) {
			logger.warn("Cannot parse {}: {}", type, json, e);
			FirebaseCrash.report(e);
			// Don't call onError(), because well-behaved observables can issue onError only once.
			// Observers probably cannot even do much with it anyway.
			// observer.onError(e);
		}

	}
}
