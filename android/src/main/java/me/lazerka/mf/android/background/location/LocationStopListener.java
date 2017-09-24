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

package me.lazerka.mf.android.background.location;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import com.baraded.mf.logging.LogService;
import com.baraded.mf.logging.Logger;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.di.Injector;
import me.lazerka.mf.android.location.LocationService;
import me.lazerka.mf.api.object.LocationRequestFromServer;
import me.lazerka.mf.api.object.LocationResponse;

import javax.inject.Inject;
import java.io.IOException;

import static android.app.PendingIntent.FLAG_CANCEL_CURRENT;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static me.lazerka.mf.android.background.location.LocationUpdateListener.EXTRA_GCM_REQUEST;

/**
 * Stops location updates.
 * Also hides notification, and cancels later scheduled stop.
 */
public class LocationStopListener extends IntentService {
	private static final Logger log = LogService.getLogger(LocationStopListener.class);

	private static final String LISTENER_TO_STOP = "LISTENER_TO_STOP";
	private static final String REQUESTER_CODE = "REQUESTER_CODE";
	private static final String NOTIFICATION_ID = "NOTIFICATION_ID";
	private static final String NOTIFICATION_TAG = "NOTIFICATION_TAG";

	@Inject
	LocationService locationService;

	public static Intent makeIntent(
			Context context,
			Intent listenerToStop,
			int requesterCode,
			int notificationId,
			String notificationTag
	) {
		Intent intent = new Intent(context, LocationStopListener.class);
		intent.putExtra(LISTENER_TO_STOP, checkNotNull(listenerToStop));
		intent.putExtra(REQUESTER_CODE, requesterCode);
		intent.putExtra(NOTIFICATION_ID, notificationId);
		intent.putExtra(NOTIFICATION_TAG, checkNotNull(notificationTag));
		return intent;
	}

	public LocationStopListener() {
		super(LocationStopListener.class.getSimpleName());
		Injector.applicationComponent().inject(this);
	}

	@Override
	public void onHandleIntent(Intent intent) {
		Intent listenerToStop = intent.getParcelableExtra(LISTENER_TO_STOP);
		int requesterCode = intent.getIntExtra(REQUESTER_CODE, -1);
		int notificationId = intent.getIntExtra(NOTIFICATION_ID, -1);
		String notificationTag = checkNotNull(intent.getStringExtra(NOTIFICATION_TAG));
		checkNotNull(listenerToStop);
		checkArgument(requesterCode != -1);
		checkArgument(notificationId != -1);
		checkNotNull(notificationTag);

		log.info("Stopping location listener for requesterCode {}", requesterCode);

		// Stop location updates.
		PendingIntent listenerPendingIntent =
				PendingIntent.getService(this, requesterCode, listenerToStop, FLAG_CANCEL_CURRENT);
		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locationManager.removeUpdates(listenerPendingIntent);

		// Hide notification.
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(notificationTag, notificationId);

		// We may have scheduled this self service for a later time, remove it.
		PendingIntent selfPendingIntent = PendingIntent.getService(this, requesterCode, intent, FLAG_CANCEL_CURRENT);
		AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(selfPendingIntent);


		// Send requester final "complete" update.
		byte[] json = listenerToStop.getByteArrayExtra(EXTRA_GCM_REQUEST);
		if (json == null) {
			log.warn("Extra" + EXTRA_GCM_REQUEST + " is null");
			return;
		}

		try {
			LocationRequestFromServer originalRequest =
					Application.getJsonMapper().readValue(json, LocationRequestFromServer.class);
			locationService
					.sendLocationUpdate(LocationResponse.complete(), originalRequest.getUpdatesTopic());
		} catch (IOException e) {
			log.error(e);
		}
	}
}
