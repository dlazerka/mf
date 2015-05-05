package me.lazerka.mf.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import me.lazerka.mf.api.gcm.LocationRequestGcmPayload;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
	private String requestId;

	@JsonProperty
	private Location location;

	@JsonProperty
	private String requesterEmail;

	// For Jackson.
	private MyLocation() {}

	public MyLocation(@Nonnull String requestId, @Nonnull Location location, @Nonnull String requesterEmail) {
		this.requestId = requestId;
		this.location = location;
		this.requesterEmail = requesterEmail;
	}

	@Nullable
	public String getRequestId() {
		return requestId;
	}

	@Nullable
	public Location getLocation() {
		return location;
	}

	@Nullable
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
