/*
 *     Find Us: privacy oriented location tracker for your friends and family.
 *     Copyright (C) 2015 Dzmitry Lazerka dlazerka@gmail.com
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

import android.support.annotation.WorkerThread;
import android.util.Log;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.background.location.LocationRequestHandler;
import me.lazerka.mf.api.gcm.GcmPayload;
import me.lazerka.mf.api.object.LocationRequest;
import me.lazerka.mf.api.object.LocationUpdate;
import org.acra.ACRA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.subjects.BehaviorSubject;

import javax.annotation.Nonnull;
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
	private static final Logger logger = LoggerFactory.getLogger(GcmReceiveService.class);
	private static final String TAG = GcmReceiveService.class.getSimpleName();


	private static final BehaviorSubject<LocationUpdate> locationReceivedSubject = BehaviorSubject.create();

	/**
	 * @return Interface to get {@link LocationUpdate} GCM messages by other services.
	 */
	@Nonnull
	public static Observable<LocationUpdate> getLocationReceivedObservable() {
		return locationReceivedSubject;
	}

	@WorkerThread
	@Override
	public void onMessageReceived(RemoteMessage message) {
		Map<String, String> data = message.getData();
		String type = data.get(GcmPayload.TYPE_FIELD);
		String json = data.get(GcmPayload.PAYLOAD_FIELD);

		String from = message.getFrom();
		logger.info("Received message from {}: {} ", from, type);

		if (!from.equals("769083712074")) {
			logger.warn("GCM message from unknown sender rejected: " + from);
			return;
		}
		if (type == null) {
			FirebaseCrash.logcat(Log.WARN, TAG, "Unknown message class " + data);
			return;
		}

		if (json == null) {
			logger.warn("No {} field", GcmPayload.PAYLOAD_FIELD);
			return;
		}

		try {
			switch (type) {
				case LocationUpdate.TYPE:
					LocationUpdate payload = Application.jsonMapper.readValue(json, LocationUpdate.class);
					locationReceivedSubject.onNext(payload);
					break;
				case LocationRequest.TYPE:
					LocationRequest locationRequest = Application.jsonMapper.readValue(json, LocationRequest.class);
					new LocationRequestHandler(this).handle(locationRequest);
					break;
				default:
					logger.warn("Unknown message type: " + type);
			}
		} catch (IOException e) {
			logger.warn("Cannot parse {}: {}", type, json, e);
			ACRA.getErrorReporter().handleSilentException(e);
			// Don't call onError(), because well-behaved observables can issue onError only once.
			// Observers probably cannot even do much with it anyway.
			// observer.onError(e);
		}

	}
}
