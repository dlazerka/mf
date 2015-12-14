package me.lazerka.mf.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Server's response to accepted {@link LocationRequest}.
 *
 * @author Dzmitry Lazerka
 */
public class LocationRequestResult {
	/**
	 * User's email where the request was sent.
	 */
	@JsonProperty
	private String email;

	@JsonProperty
	private List<GcmResult> gcmResults;

	// For Jackson.
	private LocationRequestResult() {}

	public LocationRequestResult(String email, List<GcmResult> gcmResults) {
		this.email = email;
		this.gcmResults = gcmResults;
	}

	@Nullable
	public String getEmail() {
		return email;
	}

	@Nullable
	public List<GcmResult> getGcmResults() {
		return gcmResults;
	}

}
