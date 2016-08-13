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

package me.lazerka.mf.gae.user;

import com.google.appengine.api.datastore.Email;
import com.google.common.base.MoreObjects;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.googlecode.objectify.annotation.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author Dzmitry Lazerka
 */
@Entity
@Cache
public class MfUser {
	private static final HashFunction SHA_256 = Hashing.sha256();

	@Id
	String googleId;

	DateTime createdDate;
	DateTime lastModDate;

	@Index
	Email email;

	/**
	 * To give ability to one of those users to see whether this user ever installed the app.
	 * This doesn't give permission to see location of those users.
	 */
	@Nullable
	Set<Email> friendEmails;

	/**
	 * For looking up existing users by contact list (server should not know user's contact list, only app users).
	 */
	@Index
	String emailSha256;

	private MfUser() {}

	/**
	 * Note that email must be normalized, see UserService.
	 */
	public MfUser(@Nonnull String id, @Nonnull EmailNormalized normalized) {
		this.googleId = checkNotNull(id);
		this.email = new Email(normalized.getEmail());
		this.emailSha256 = SHA_256.hashString(normalized.getEmail(), UTF_8).toString();
	}

	@OnSave
	void onSave() {
		checkNotNull(googleId);

		if (createdDate == null) {
			createdDate = DateTime.now(DateTimeZone.UTC);
		}
		lastModDate = DateTime.now(DateTimeZone.UTC);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		MfUser user = (MfUser) o;

		return googleId.equals(user.googleId);

	}

	@Override
	public int hashCode() {
		return googleId.hashCode();
	}

	public String getId() {
		return googleId;
	}

	public EmailNormalized getEmail() {
		return new EmailNormalized(email.getEmail());
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("googleId", googleId)
				.add("email", email)
				.toString();
	}

	/**
	 * Emails this user marked as his/her friends in the app.
	 */
	@Nullable
	public Set<EmailNormalized> getFriendEmails() {
		if (friendEmails == null) {
			return null;
		}
		HashSet<EmailNormalized> result = new HashSet<>(friendEmails.size());
		for(Email email : friendEmails) {
			result.add(new EmailNormalized(email.getEmail()));
		}
		return result;
	}

	public void setFriendEmails(@Nullable Set<EmailNormalized> friendEmails) {
		if (friendEmails == null) {
			this.friendEmails = null;
			return;
		}

		this.friendEmails = new HashSet<>(friendEmails.size());
		for(EmailNormalized normalized : friendEmails) {
			this.friendEmails.add(new Email(normalized.getEmail()));
		}
	}
}
