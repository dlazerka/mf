package me.lazerka.mf.api;

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

	public Location() {
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

	public void setLat(double lat) {
		this.lat = lat;
	}

	public double getLon() {
		return lon;
	}

	public void setLon(double lon) {
		this.lon = lon;
	}

	public float getAcc() {
		return acc;
	}

	public void setAcc(float acc) {
		this.acc = acc;
	}

	@Override
	public String toString() {
		return email + ":" + lat + "," + lon +"," + acc;
	}
}
