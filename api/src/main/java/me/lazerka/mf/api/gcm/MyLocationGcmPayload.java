package me.lazerka.mf.api.gcm;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import me.lazerka.mf.api.object.Location;

/**
 * The `data` field of a request for location that comes to a friend's device.
 *
 * @author Dzmitry Lazerka
 */
public class MyLocationGcmPayload extends GcmPayload {
	public static final String TYPE = "MyLocation";

	public static final String LOCATION = "location";
	public static final String REQUEST_ID = "requestId";

	@JsonProperty(REQUEST_ID)
	private String requestId;

	@JsonProperty(LOCATION)
	private Location location;

	// For Jackson.
	private MyLocationGcmPayload() {}

	public MyLocationGcmPayload(String requestId, Location location) {
		this.requestId = requestId;
		this.location = location;
	}

	@Override
	@JsonIgnore
	public String getType() {
		return TYPE;
	}

	public Location getLocation() {
		return location;
	}

	@Override
	public String toString() {
		return "MyLocationGcmPayload{" +
				"requestId='" + requestId + '\'' +
				", location=" + location +
				'}';
	}
}
