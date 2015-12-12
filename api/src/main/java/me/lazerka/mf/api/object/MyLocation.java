package me.lazerka.mf.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import me.lazerka.mf.api.gcm.LocationRequest;

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
public class MyLocation implements ApiObject {
	public static final String PATH = "/rest/myLocation";

	@JsonProperty
	private Location location;

	@JsonProperty
	private LocationRequest locationRequest;

	// For Jackson.
	private MyLocation() {}

	public MyLocation(@Nonnull Location location, @Nonnull LocationRequest locationRequest) {
		this.location = checkNotNull(location);
		this.locationRequest = checkNotNull(locationRequest);
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
