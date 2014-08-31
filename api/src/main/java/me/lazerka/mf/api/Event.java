package me.lazerka.mf.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class Event {

	@JsonProperty
	private String deviceId;

	@JsonProperty
	private long ms;

	protected Event() {
	}

	protected Event(String deviceId, long ms) {
		this.deviceId = deviceId;
		this.ms = ms;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public long getMs() {
		return ms;
	}

	public void setMs(long ms) {
		this.ms = ms;
	}

	@JsonIgnore
	public String getPath() {
		return "/d=" + getDeviceId() + "/ms=" + getMs();
	}
}
