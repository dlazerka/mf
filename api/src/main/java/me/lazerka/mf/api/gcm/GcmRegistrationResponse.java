package me.lazerka.mf.api.gcm;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Our server's response on saved GCM registration token.
 * @author Dzmitry Lazerka
 */
public class GcmRegistrationResponse {
	@JsonProperty
	private boolean success = true;
}
