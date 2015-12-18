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

package me.lazerka.mf.android.adapter;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Bean for holding emails together with contact info.
 *
 * Shallow-immutable.
 *
 * @author Dzmitry Lazerka
 */
public class FriendInfo implements Parcelable {
	public long id;

	public String lookupKey;

	public String displayName;

	@Nullable
	public String photoUri;

	public final Set<String> emails = new HashSet<>();

	protected FriendInfo(Parcel in) {
		id = in.readLong();
		lookupKey = in.readString();
		displayName = in.readString();
		photoUri = in.readString();
	}

	// For Jackson
	private FriendInfo() {}

	public FriendInfo(
			long id,
			@Nonnull String lookupKey,
			@Nonnull String displayName,
			@Nullable String photoUri,
			@Nonnull Collection<String> emails
	) {
		this.id = id;
		this.lookupKey = checkNotNull(lookupKey);
		this.displayName = checkNotNull(displayName);
		this.photoUri = photoUri;
		this.emails.addAll(checkNotNull(emails));
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(id);
		dest.writeString(lookupKey);
		dest.writeString(displayName);
		dest.writeString(photoUri);
		dest.writeStringList(new ArrayList<>(emails));
	}

	public static final Creator<FriendInfo> CREATOR = new Creator<FriendInfo>() {
		@Override
		public FriendInfo createFromParcel(Parcel in) {
			return new FriendInfo(in);
		}

		@Override
		public FriendInfo[] newArray(int size) {
			return new FriendInfo[size];
		}
	};
}
