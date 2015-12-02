package me.lazerka.mf.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nonnull;

/**
 * @author Dzmitry Lazerka
 */
public class GcmRegistration implements ApiObject {
	public static final String PATH = "/rest/gcm/registration";

	/**
	 * Max length: 4k (max length of a cookie).
	 */
	@JsonProperty
	private String id;

	/** As specified in AndroidManifest.xml */
	@JsonProperty
	private int appVersion;

	@Override
	public String getPath() {
		return PATH;
	}

	// For Jackson.
	private GcmRegistration() {
	}

	public GcmRegistration(@Nonnull String id, int appVersion) {
		this.id = id;
		this.appVersion = appVersion;
	}

	public String getId() {
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
