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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import me.lazerka.mf.api.gcm.GcmPayload;
import org.joda.time.Duration;

import javax.annotation.Nullable;

/**
 * Response sent in response location request.
 *
 * {@link #location} can be null on the first response, which just let's requester know that request was approved.
 *
 * {@link #duration} can be different that what was requested, if changed by receiver.
 *
 * @author Dzmitry Lazerka
 */
@JsonInclude(Include.NON_DEFAULT)
public class LocationResponse implements GcmPayload {
	public static final String TYPE = "LocationUpdate";

	@JsonProperty
	private String googleAuthToken;

	@Nullable
	@JsonProperty
	private Location location;

	@JsonProperty
	private Duration duration;

	@JsonProperty
	private Error error;

	// For Jackson.
	private LocationResponse() {}

	public static LocationResponse denied() {
		LocationResponse result = new LocationResponse();
		result.error = new Error("denied", null);
		return result;
	}

	public LocationResponse(
			@Nullable Location location,
			Duration duration
	) {
		this.location = location;
		this.duration = duration;
	}

	@Override
	@JsonIgnore
	public String getType() {
		return TYPE;
	}

	public String getGoogleAuthToken() {
		return googleAuthToken;
	}

	@Nullable
	public Location getLocation() {
		return location;
	}

	public boolean isSuccessful() {
		return error != null;
	}

	public Duration getDuration() {
		return duration;
	}

	public Error getError() {
		return error;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				// No googleAuthToken
				.add("location", location)
				.add("duration", duration)
				.add("error", error)
				.toString();
	}
}
