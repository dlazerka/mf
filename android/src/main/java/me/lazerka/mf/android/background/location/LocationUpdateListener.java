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

import android.app.IntentService;
import android.content.Intent;
import android.location.LocationManager;
import com.baraded.mf.logging.LogService;
import com.baraded.mf.logging.Logger;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationResult;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.api.object.Location;
import me.lazerka.mf.api.object.LocationRequestFromServer;
import me.lazerka.mf.api.object.LocationResponse;
import org.joda.time.DateTime;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Receives location updates that were scheduled in {@link LocationRequestHandler},
 * and sends them to server.
 *
 * Is invoked on each location update.
 */
public class LocationUpdateListener extends IntentService {
	private static final Logger logger = LogService.getLogger(LocationUpdateListener.class);

	static final String EXTRA_GCM_REQUEST = "gcmRequest";

	public LocationUpdateListener() {
		super(LocationUpdateListener.class.getSimpleName());
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		// May be sent by FusedLocationApi
		if (LocationAvailability.hasLocationAvailability(intent)) {
			LocationAvailability availability = LocationAvailability.extractLocationAvailability(intent);
			logger.trace(availability.toString());
		}

		android.location.Location location = null;

		// From FusedLocationApi.
		if (LocationResult.hasResult(intent)) {
			//logger.info("Sending LocationResult");

			// May throw
			// java.lang.ClassCastException: android.location.Location cannot be cast to
			// com.google.android.gms.location.LocationResult
			//
			// Also see https://code.google.com/p/android/issues/detail?id=81812
			try {
				location = LocationResult.extractResult(intent).getLastLocation();
			} catch (ClassCastException e) {
				location = intent.getExtras().getParcelable("com.google.android.gms.location.EXTRA_LOCATION_RESULT");
			}
		}
		// From LocationManager.
		else if (intent.hasExtra(LocationManager.KEY_LOCATION_CHANGED)) {
			//logger.info("Sending Location");
			location = intent.getExtras().getParcelable(LocationManager.KEY_LOCATION_CHANGED);
		}

		if (location != null) {
			try {
				byte[] json = checkNotNull(intent.getByteArrayExtra(EXTRA_GCM_REQUEST));
				LocationRequestFromServer gcmRequest = Application.getJsonMapper()
						.readValue(json, LocationRequestFromServer.class);

				Location locationBean = new Location(
						new DateTime(location.getTime()),
						location.getLatitude(),
						location.getLongitude(),
						location.getAccuracy()
				);

				LocationResponse locationResponse = new LocationResponse(locationBean, gcmRequest.getDuration());
				Application.getLocationService().sendLocationUpdate(
						locationResponse,
						gcmRequest.getUpdatesTopic()
				);

			} catch (IOException e) {
				// Unrealistic, we already parsed it once in GcmReceiveService.
				logger.error("Cannot parse", e);
			}
		}
	}
}
