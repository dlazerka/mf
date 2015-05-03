package me.lazerka.mf.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Server's response to accepted {@link MyLocation}.
 *
 * @author Dzmitry Lazerka
 */
public class MyLocationResponse {
	@JsonProperty
	private final String requestId;

	public MyLocationResponse(String requestId) {
		this.requestId = requestId;
	}

	public String getRequestId() {
		return requestId;
	}
}
