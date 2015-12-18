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

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calls {@link LocationManager#removeUpdates(PendingIntent)}.
 */
public class LocationStopListener extends BroadcastReceiver {
	private static final Logger logger = LoggerFactory.getLogger(LocationStopListener.class);

	private static final String LISTENER_TO_STOP = "LISTENER_TO_STOP";

	public static Intent makeIntent(Context context, PendingIntent listenerToStop) {
		Intent intent = new Intent(context, LocationStopListener.class);
		intent.putExtra(LISTENER_TO_STOP, listenerToStop);
		return intent;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		PendingIntent listenerToStop = intent.getParcelableExtra(LISTENER_TO_STOP);
		logger.info("Stopping location listener");

		LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		locationManager.removeUpdates(listenerToStop);
	}
}
