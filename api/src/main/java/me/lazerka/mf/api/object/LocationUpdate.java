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
