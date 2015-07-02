package me.lazerka.mf.gae.entity;

import com.google.appengine.api.users.User;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

	User user;
	@Index String email;

	/**
	 * To give ability to one of those users to see whether this user ever installed the app.
	 * This doesn't give permission to see location of those users.
	 */
	@Nullable
	Set<String> friendEmails;

	/**
	 * For looking up existing users by contact list (server should not know user's contact list, only app users).
	 */
	@Index String emailSha256;

	private MfUser() {}

	public MfUser(@Nonnull User user) {
		this.googleId = checkNotNull(user.getUserId());
		this.user = user;
		this.email = checkNotNull(user.getEmail());
		this.emailSha256 = SHA_256.hashString(email, UTF_8).toString();
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

		if (!googleId.equals(user.googleId)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		return googleId.hashCode();
	}

	public Key<MfUser> key() {
		return Key.create(MfUser.class, googleId);
	}

	public String getId() {
		return googleId;
	}

	public String getEmail() {
		return user.getEmail();
	}

	public User getUser() {
		return user;
	}

	@Nullable
	public Set<String> getFriendEmails() {
		return friendEmails;
	}

	public void setFriendEmails(@Nullable Set<String> friendEmails) {
		this.friendEmails = friendEmails;
	}
}
