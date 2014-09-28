package me.lazerka.mf.api.gcm;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Our server's response on saved GCM registration token.
 * @author Dzmitry Lazerka
 */
public class GcmRegistrationResponse {
	/**
	 * Server-generated ID of the registration.
	 * Isn't used anywhere currently, just to know it's stored.
	 */
	@JsonProperty
	private String id;

	public GcmRegistrationResponse() {
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
