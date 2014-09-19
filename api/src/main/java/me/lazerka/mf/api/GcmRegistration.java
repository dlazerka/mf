package me.lazerka.mf.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Dzmitry Lazerka
 */
public class GcmRegistration implements ApiObject {
	public static final String PATH = "/rest/gcm/registration";

	@JsonProperty
	private final String id;

	public GcmRegistration(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		return "GcmRegistration{" +
				"id='" + id + '\'' +
				'}';
	}
}
