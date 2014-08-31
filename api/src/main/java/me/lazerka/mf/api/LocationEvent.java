package me.lazerka.mf.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LocationEvent extends Event {
	public static final String PATH = "/location";

	@JsonProperty
	private double lat;

	@JsonProperty
	private double lon;

	@JsonProperty
	private float acc;

	public LocationEvent() {
	}

	public LocationEvent(String deviceId, long ms) {
		super(deviceId, ms);
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
		return "{" + lat + ", " + lon +", " + acc + "}";
	}
}
