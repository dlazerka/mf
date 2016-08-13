/*
 *     Find Us: privacy oriented location tracker for your friends and family.
 *     Copyright (C) 2016 Dzmitry Lazerka dlazerka@gmail.com
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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import org.joda.time.Duration;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A request for location updates that one user sends to backend server.
 *
 * Server will authenticate sender and will send {@link LocationRequestFromServer} to the friend.
 *
 * @author Dzmitry Lazerka
 */
public class LocationRequest2 {
	public static final String TYPE = "LocationRequest";

	@JsonProperty
	private String googleAuthToken;

	@JsonProperty
	private String updatesTopic;

	/** For how long user wants to receive location updates from their friend. */
	@JsonProperty
	private Duration duration;

	// For Jackson.
	public LocationRequest2() {}

	public LocationRequest2(String googleAuthToken, String updatesTopic, Duration duration) {
		this.googleAuthToken = checkNotNull(googleAuthToken);
		this.updatesTopic = checkNotNull(updatesTopic);
		this.duration = checkNotNull(duration);
	}

	public String getGoogleAuthToken() {
		return googleAuthToken;
	}

	public String getUpdatesTopic() {
		return updatesTopic;
	}

	public Duration getDuration() {
		return duration;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				// No googleAuthToken
				.add("updatesTopic", updatesTopic)
				.add("duration", duration)
				.toString();
	}
}
