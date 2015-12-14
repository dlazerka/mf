package me.lazerka.mf.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import java.util.List;

/**
 * Server's response to accepted {@link LocationUpdate}.
 *
 * @author Dzmitry Lazerka
 */
public class LocationUpdateResponse {
	@JsonProperty
	private LocationUpdate locationUpdate;

	@JsonProperty
	private List<GcmResult> gcmResults;

	// For Jackson.
	private LocationUpdateResponse() {}

	public LocationUpdateResponse(LocationUpdate locationUpdate, List<GcmResult> gcmResults) {

		this.locationUpdate = locationUpdate;
		this.gcmResults = gcmResults;
	}

	public LocationUpdate getLocationUpdate() {
		return locationUpdate;
	}

	public List<GcmResult> getGcmResults() {
		return gcmResults;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
		                  .add("locationUpdate", locationUpdate)
		                  .add("gcmResults", gcmResults)
		                  .toString();
	}
}
