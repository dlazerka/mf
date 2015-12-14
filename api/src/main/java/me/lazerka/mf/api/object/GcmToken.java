package me.lazerka.mf.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nonnull;

/**
 * @author Dzmitry Lazerka
 */
public class GcmToken implements ApiObject {
	public static final String PATH = "/rest/gcm/token";

	/**
	 * Max length: 4k (max length of a cookie).
	 */
	@JsonProperty
	private String token;

	/** As specified in AndroidManifest.xml */
	@JsonProperty
	private int appVersion;

	@Override
	public String getPath() {
		return PATH;
	}

	// For Jackson.
	private GcmToken() {
	}

	public GcmToken(@Nonnull String token, int appVersion) {
		this.token = token;
		this.appVersion = appVersion;
	}

	public String getToken() {
		return token;
	}

	public int getAppVersion() {
		return appVersion;
	}

	@Override
	public String toString() {
		return "GcmToken{" +
				"id=<removed>" +
				", appVersion='" + appVersion + '\'' +
				'}';

	}
}
