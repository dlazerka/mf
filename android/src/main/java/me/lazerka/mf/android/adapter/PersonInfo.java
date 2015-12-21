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

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;

import java.util.Collection;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Bean for holding emails together with contact info.
 *
 * Shallow-immutable.
 *
 * @author Dzmitry Lazerka
 */
public class PersonInfo implements Parcelable {
	public final long id;

	@Nonnull
	public final String lookupKey;

	@Nonnull
	public final Uri lookupUri;

	@Nonnull
	public final String displayName;

	@Nullable
	public final String photoUri;

	public final ImmutableSet<String> emails;

	public PersonInfo(
			long id,
			@Nonnull String lookupKey,
			@Nonnull Uri lookupUri,
			@Nonnull String displayName,
			@Nullable String photoUri,
			@Nonnull Collection<String> emails
	) {
		this.id = id;
		this.lookupKey = checkNotNull(lookupKey);
		this.lookupUri = checkNotNull(lookupUri);
		this.displayName = checkNotNull(displayName);
		this.photoUri = photoUri;
		this.emails = ImmutableSet.copyOf(emails);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof PersonInfo)) return false;
		PersonInfo that = (PersonInfo) o;
		return lookupKey.equals(that.lookupKey);
	}

	@Override
	public int hashCode() {
		return lookupKey.hashCode();
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("id", id)
				.add("displayName", displayName)
				.add("emails", emails)
				.toString();
	}

	//////
	// For Parcelable
	//////

	protected PersonInfo(Parcel in) {
		id = in.readLong();
		lookupKey = in.readString();
		lookupUri = in.readParcelable(Uri.class.getClassLoader());
		displayName = in.readString();
		photoUri = in.readString();
		emails = ImmutableSet.copyOf(in.createStringArray());
	}

	public static final Creator<PersonInfo> CREATOR = new Creator<PersonInfo>() {
		@Override
		public PersonInfo createFromParcel(Parcel in) {
			return new PersonInfo(in);
		}

		@Override
		public PersonInfo[] newArray(int size) {
			return new PersonInfo[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(id);
		dest.writeString(lookupKey);
		dest.writeParcelable(lookupUri, flags);
		dest.writeString(displayName);
		dest.writeString(photoUri);
		dest.writeStringArray(emails.toArray(new String[emails.size()]));
	}
}
