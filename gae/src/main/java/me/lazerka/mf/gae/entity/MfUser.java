package me.lazerka.mf.gae.entity;

import com.google.appengine.api.users.User;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dzmitry Lazerka
 */
@Entity
@Cache
public class MfUser {
	@Id
	String googleId;

	DateTime createdDate;
	DateTime lastModDate;

	@Index User user;
	@Index String email;

	private MfUser() {}

	public MfUser(@Nonnull User user) {
		this.googleId = checkNotNull(user.getUserId());
		this.user = user;
		this.email = checkNotNull(user.getEmail());
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
}
