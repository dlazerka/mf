package me.lazerka.mf.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

/**
 * Server's response to accepted {@link MyLocation}.
 *
 * @author Dzmitry Lazerka
 */
public class MyLocationResponse {
	@JsonProperty
	private String requestId;

	// For Jackson.
	private MyLocationResponse() {}

	public MyLocationResponse(String requestId) {
		this.requestId = requestId;
	}

	public String getRequestId() {
		return requestId;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("requestId", requestId)
				.toString();
	}
}
