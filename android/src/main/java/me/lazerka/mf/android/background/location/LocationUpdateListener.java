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

import android.app.IntentService;
import android.content.Intent;
import android.location.LocationManager;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationResult;
import com.squareup.okhttp.Response;

import org.acra.ACRA;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.auth.GoogleApiException;
import me.lazerka.mf.android.auth.SignInManager;
import me.lazerka.mf.android.background.ApiPost;
import me.lazerka.mf.api.object.GcmResult;
import me.lazerka.mf.api.object.Location;
import me.lazerka.mf.api.object.LocationRequest;
import me.lazerka.mf.api.object.LocationUpdate;
import me.lazerka.mf.api.object.LocationUpdateResponse;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.joda.time.DateTimeZone.UTC;

/**
 * Receives location updates that were scheduled in {@link LocationRequestHandler},
 * and sends them to server.
 *
 * Is invoked on each location update.
 */
public class LocationUpdateListener extends IntentService {
	private static final Logger logger = LoggerFactory.getLogger(LocationUpdateListener.class);

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
			logger.info("Got LocationResult"); // todo trace
			location = LocationResult.extractResult(intent).getLastLocation();
		}
		// From LocationManager.
		else if (intent.hasExtra(LocationManager.KEY_LOCATION_CHANGED)) {
			logger.info("Got Location"); // todo trace
			location = intent.getExtras().getParcelable(LocationManager.KEY_LOCATION_CHANGED);
		}

		if (location != null) {
			try {
				byte[] json = checkNotNull(intent.getByteArrayExtra(EXTRA_GCM_REQUEST));
				LocationRequest gcmRequest = Application.jsonMapper.readValue(json, LocationRequest.class);

				sendLocation(location, gcmRequest);

			} catch (IOException e) {
				// Unrealistic, we already parsed it once in GcmReceiveService.
				logger.error("Cannot parse", e);
				ACRA.getErrorReporter().handleSilentException(e);
			}
		}
	}

	void sendLocation(android.location.Location location, LocationRequest gcmRequest) {
		SignInManager signInManager = new SignInManager();
		GoogleSignInAccount account;
		try {
			account = signInManager.getAccountBlocking(this);
		} catch (GoogleApiException e) {
			logger.warn("Unable to sign in: {} {}", e.getCode(), e.getMessage());
			// TODO implement reconnection logic
			return;
		}

		Location locationBean = new Location(
				DateTime.now(UTC),
				account.getEmail(),
				location.getLatitude(),
				location.getLongitude(),
				location.getAccuracy()
		);

		LocationUpdate locationUpdate = new LocationUpdate(locationBean, gcmRequest);

		ApiPost post = new ApiPost(locationUpdate);
		try {
			Response response = post.newCall(account).execute();

			if (response.code() == 200) {
				String json = response.body().string();
				LocationUpdateResponse bean =
						Application.jsonMapper.readValue(json, LocationUpdateResponse.class);
				List<GcmResult> gcmResults = bean.getGcmResults();

				for(GcmResult gcmResult : gcmResults) {
					if (!gcmResult.isSuccessful()) {
						logger.warn("Unsuccessful sending: " + gcmResult.getError());
						break;
					}
				}
			} else {
				logger.warn("Failed: {}, {}", response.code(), response.message());
			}
		} catch (IOException e) {
			logger.warn("IOException: {}", e.getMessage());
			// Not sending ACRA, because this is spammy.
		}
	}
}
