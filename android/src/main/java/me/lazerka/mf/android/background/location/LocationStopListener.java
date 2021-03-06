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

package me.lazerka.mf.android.background.location;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static android.app.PendingIntent.FLAG_CANCEL_CURRENT;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Stops location updates.
 * Also hides notification, and cancels later scheduled stop.
 */
public class LocationStopListener extends IntentService {
	private static final Logger logger = LoggerFactory.getLogger(LocationStopListener.class);

	private static final String LISTENER_TO_STOP = "LISTENER_TO_STOP";
	private static final String REQUESTER_CODE = "REQUESTER_CODE";
	private static final String NOTIFICATION_ID = "NOTIFICATION_ID";
	private static final String NOTIFICATION_TAG = "NOTIFICATION_TAG";

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

		logger.info("Stopping location listener for requesterCode {}", requesterCode);

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
	}
}
