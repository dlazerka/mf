package me.lazerka.mf.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import java.util.Set;

/**
 * A request that requester user sends to our server.
 *
 * @author Dzmitry Lazerka
 */
public class LocationRequest {
	public static final String PATH = "/rest/locationRequest";

	@JsonProperty
	private final String requestId;

	@JsonProperty
	private final Set<String> emails;

	public LocationRequest(String requestId, Set<String> emails) {
		this.requestId = requestId;
		this.emails = emails;
	}

	public String getRequestId() {
		return requestId;
	}

	public Set<String> getEmails() {
		return emails;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("requestId", requestId)
				.add("emails", emails)
				.toString();
	}
}
