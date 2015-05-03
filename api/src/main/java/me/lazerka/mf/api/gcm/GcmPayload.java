package me.lazerka.mf.api.gcm;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Dzmitry Lazerka
 */
public abstract class GcmPayload {
	/**
	 * Distinguisher of GCM messages we send.
	 */
	public static final String TYPE = "type";

	@JsonProperty(TYPE)
	public abstract String getType();
}
