package me.lazerka.mf.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nonnull;

/**
 * @author Dzmitry Lazerka
 */
public class GcmRegistration implements ApiObject {
	public static final String PATH = "/rest/gcm/registration";

	@JsonProperty
	private String id;

	/** As specified in AndroidManifest.xml */
	@JsonProperty
	private int appVersion;

	// For Jackson.
	private GcmRegistration() {
	}

	public GcmRegistration(@Nonnull String id, int appVersion) {
		this.id = id;
		this.appVersion = appVersion;
	}

	public String getToken() {
		return id;
	}

	@Override
	public String toString() {
		return "GcmRegistration{" +
				"id=<removed>" +
				", appVersion='" + appVersion + '\'' +
				'}';

	}

	public int getAppVersion() {
		return appVersion;
	}
}
