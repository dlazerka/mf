/*
 *     Copyright (C) 2017 Dzmitry Lazerka
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
	private double lat;

	@JsonProperty
	private double lon;

	@JsonProperty
	private float acc;

	// For Jackson.
	private Location() {}

	public Location(DateTime when, double lat, double lon, float acc) {
		this.when = when;
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
		return lat + "," + lon + "," + acc + "," + when;
	}
}
