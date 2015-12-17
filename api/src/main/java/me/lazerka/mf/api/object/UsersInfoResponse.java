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

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @see UsersInfoRequest
 *
 * @author Dzmitry Lazerka
 */
public class UsersInfoResponse {
	@JsonProperty
	private List<UserInfo> userInfos;

	// For Jackson.
	private UsersInfoResponse() {}

	public UsersInfoResponse(@Nonnull List<UserInfo> userInfos) {
		this.userInfos = checkNotNull(userInfos);
	}

	/**
	 * Key is server response, value is users request (non-canonicalized).
	 */
	@Nonnull
	public List<UserInfo> getUserInfos() {
		return userInfos == null ? Collections.<UserInfo>emptyList() : userInfos;
	}

	@Override
	public String toString() {
		return userInfos.toString();
	}
}
