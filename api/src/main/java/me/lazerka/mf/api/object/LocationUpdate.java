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

package me.lazerka.mf.api.object;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import me.lazerka.mf.api.gcm.GcmPayload;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Response sent in response location request.
 *
 * Location request was received by device from GCM (see {@link LocationRequest}),
 * and sent to our server through regular HTTP.
 *
 * @author Dzmitry Lazerka
 */
public class LocationUpdate implements ApiObject, GcmPayload {
	public static final String PATH = "/rest/locationUpdate";
	public static final String TYPE = "LocationUpdate";

	public static final String LOCATION = "location";
	public static final String LOCATION_REQUEST = "locationRequest";

	@JsonProperty(LOCATION)
	private Location location;

	@JsonProperty(LOCATION_REQUEST)
	private LocationRequest locationRequest;

	// For Jackson.
	private LocationUpdate() {}

	public LocationUpdate(@Nonnull Location location, @Nonnull LocationRequest locationRequest) {
		this.location = checkNotNull(location);
		this.locationRequest = checkNotNull(locationRequest);
	}

	@Override
	@JsonIgnore
	public String getType() {
		return TYPE;
	}

	@Override
	public String getPath() {
		return PATH;
	}

	@Nullable
	public Location getLocation() {
		return location;
	}

	@Nullable
	public LocationRequest getLocationRequest() {
		return locationRequest;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("location", location)
				.add("locationRequest", locationRequest)
				.toString();
	}
}
