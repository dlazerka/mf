/*
 *     Find Us: privacy oriented location tracker for your friends and family.
 *     Copyright (C) 2016 Dzmitry Lazerka dlazerka@gmail.com
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

package me.lazerka.mf.android.location;

import com.google.common.base.MoreObjects;
import me.lazerka.mf.android.adapter.PersonInfo;
import me.lazerka.mf.api.object.LocationResponse;

public class FriendLocationResponse {
	PersonInfo contact;
	LocationResponse response;

	public FriendLocationResponse(PersonInfo contact, LocationResponse response) {
		this.contact = contact;
		this.response = response;
	}

	public PersonInfo getContact() {
		return contact;
	}

	public LocationResponse getResponse() {
		return response;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("contact", contact)
				.add("response", response)
				.toString();
	}
}
