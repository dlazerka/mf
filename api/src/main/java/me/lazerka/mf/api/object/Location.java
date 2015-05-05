package me.lazerka.mf.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

/**
 * @author Dzmitry Lazerka
 */
public class Location {
	public static final String PATH = "/rest/location";

	@JsonProperty
	private DateTime when;

	@JsonProperty
	private String email;

	@JsonProperty
	private double lat;

	@JsonProperty
	private double lon;

	@JsonProperty
	private float acc;

	// For Jackson.
	private Location() {}

	public Location(DateTime when, String email, double lat, double lon, float acc) {
		this.when = when;
		this.email = email;
		this.lat = lat;
		this.lon = lon;
		this.acc = acc;
	}

	public DateTime getWhen() {
		return when;
	}

	public void setWhen(DateTime when) {
		this.when = when;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public double getLat() {
		return lat;
	}

	public double getLon() {
		return lon;
	}

	public float getAcc() {
		return acc;
	}

	@Override
	public String toString() {
		return email + ":" + lat + "," + lon + "," + acc + "," + when;
	}
}
