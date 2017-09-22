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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import me.lazerka.mf.api.gcm.GcmPayload;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * A request for location updates that one user sends to backend server to be sent to another user.
 * Is both upstream and downstream message.
 *
 * Receiver user device authorizes each request, and only for `duration` given.
 *
 * @author Dzmitry Lazerka
 */
@JsonInclude(Include.NON_NULL)
public class LocationRequest implements ApiObject, GcmPayload {
	public static final String PATH = "/rest/locationRequest";
	public static final String TYPE = "LocationRequest";

	@JsonProperty
	private String requestId;

	/**
	 * Who's asking for location.
	 *
	 * Server must verify this matches user real OAuth identity.
	 * Receiver user must approve/reject authorize this request.
	 */
	@JsonProperty
	private String requesterEmail;

	@JsonProperty
	private Set<String> emails;

	/**
	 * When the message was received by server from the requester.
	 */
	@JsonProperty
	private DateTime sentAt;

	/** For how long user wants to receive location updates from their friend. */
	@JsonProperty
	private Duration duration;

	// For Jackson.
	private LocationRequest() {}

	public LocationRequest(String requestId, Set<String> emails, DateTime sentAt, Duration duration) {
		this.requestId = requestId;
		this.emails = emails;
		this.sentAt = sentAt;
		this.duration = duration;
	}

	public LocationRequest(String requestId, String requesterEmail, DateTime sentAt, Duration duration) {
		this.requestId = requestId;
		this.requesterEmail = requesterEmail;
		this.sentAt = sentAt;
		this.duration = duration;
	}

	@Override
	public String getPath() {
		return PATH;
	}

	@Override
	public String getType() {
		return TYPE;
	}

	public String getRequestId() {
		return requestId;
	}

	@Nullable
	public String getRequesterEmail() {
		return requesterEmail;
	}

	@Nullable
	public Set<String> getEmails() {
		return emails;
	}

	@Nullable
	public DateTime getSentAt() {
		return sentAt;
	}

	@Nullable
	public Duration getDuration() {
		return duration;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("requestId", requestId)
				.add("requesterEmail", requesterEmail)
				.add("emails", emails)
				.add("sentAt", sentAt)
				.add("duration", duration)
				.toString();
	}
}
