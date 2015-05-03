package me.lazerka.mf.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import me.lazerka.mf.api.gcm.LocationRequestGcmPayload;

/**
 * Response sent in response location request.
 *
 * Location request was received by device from GCM (see {@link LocationRequestGcmPayload}),
 * and sent to our server through regular HTTP.
 *
 * @author Dzmitry Lazerka
 * @see LocationRequestGcmPayload
 */
public class MyLocation {
	public static final String PATH = "/rest/myLocation";

	@JsonProperty
	private final String requestId;

	@JsonProperty
	private final Location location;

	@JsonProperty
	private final String requesterEmail;

	public MyLocation(String requestId, Location location, String requesterEmail) {
		this.requestId = requestId;
		this.location = location;
		this.requesterEmail = requesterEmail;
	}

	public String getRequestId() {
		return requestId;
	}

	public Location getLocation() {
		return location;
	}

	public String getRequesterEmail() {
		return requesterEmail;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("requestId", requestId)
				.add("location", location)
				.add("requesterEmail", requesterEmail)
				.toString();
	}
}
