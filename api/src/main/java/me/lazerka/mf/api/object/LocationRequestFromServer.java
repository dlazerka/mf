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
import me.lazerka.mf.api.gcm.GcmPayload;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import javax.annotation.Nullable;

/**
 * Location request that server sends to a user.
 *
 * Server has verified {@link #requesterEmail}, so client trust it. That's the only reason to have server at all.
 *
 * Receiver should send location updates to given short-lived {@link #updatesTopic},
 * that is a shared secret between requester and receiver.
 *
 *  @author Dzmitry Lazerka
 */
public class LocationRequestFromServer implements GcmPayload {
	public static final String TYPE = "LocationRequest";

	/**
	 * Who's asking for location.
	 *
	 * Server must verify this matches user real OAuth identity.
	 * Receiver user must approve/reject authorize this request.
	 */
	@JsonProperty("requesterEmail")
	private String requesterEmail;

	@JsonProperty("updatesTopic")
	private String updatesTopic;

	/** For how long user wants to receive location updates from their friend. */
	@JsonProperty("duration")
	private Duration duration;

	@JsonProperty("requestedAt")
	private DateTime requestedAt;

	// For Jackson.
	public LocationRequestFromServer() {}

	public LocationRequestFromServer(
			String requesterEmail,
			String updatesTopic,
			Duration duration,
			DateTime requestedAt)
	{
		this.requesterEmail = requesterEmail;
		this.updatesTopic = updatesTopic;
		this.duration = duration;
		this.requestedAt = requestedAt;
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Nullable
	public String getRequesterEmail() {
		return requesterEmail;
	}

	public String getUpdatesTopic() {
		return updatesTopic;
	}

	@Nullable
	public Duration getDuration() {
		return duration;
	}

	public DateTime getRequestedAt() {
		return requestedAt;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("requesterEmail", requesterEmail)
				.add("updatesTopic", updatesTopic)
				.add("duration", duration)
				.add("requestedAt", requestedAt)
				.toString();
	}
}
