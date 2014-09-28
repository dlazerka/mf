package me.lazerka.mf.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

/**
 * A request that user's device sends to our server.
 *
 * @author Dzmitry Lazerka
 */
public class LocationRequest {
	public static final String PATH = "/rest/locationRequest";

	@JsonProperty
	private Set<String> emails;

	public Set<String> getEmails() {
		return emails;
	}

	public void setEmails(Set<String> emails) {
		this.emails = emails;
	}

	@Override
	public String toString() {
		return "LocationRequest{" +
				"emails=" + emails +
				'}';
	}
}
