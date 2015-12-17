/*
 *     Find Us: privacy oriented location tracker for your friends and family.
 *     Copyright (C) 2015 Dzmitry Lazerka dlazerka@gmail.com
 *
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

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
