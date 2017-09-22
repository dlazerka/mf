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
public class LocationRequest2 implements ApiObject {
	public static final String PATH = "/rest/locationRequest";

	@JsonProperty
	private String updatesTopic;

	@JsonProperty
	private UserFindId to;

	/** For how long user wants to receive location updates from their friend. */
	@JsonProperty
	private Duration duration;

	// For Jackson.
	public LocationRequest2() {}

	public LocationRequest2(String updatesTopic, UserFindId to, Duration duration) {
		this.updatesTopic = checkNotNull(updatesTopic);
		this.to = checkNotNull(to);
		this.duration = checkNotNull(duration);
	}

	/**
	 * Where to send location updates.
	 */
	public String getUpdatesTopic() {
		return updatesTopic;
	}

	public UserFindId getTo() {
		return to;
	}

	public Duration getDuration() {
		return duration;
	}

	@Override
	public String getPath() {
		return PATH;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("updatesTopic", updatesTopic)
				.add("to", to)
				.add("duration", duration)
				.toString();
	}
}
