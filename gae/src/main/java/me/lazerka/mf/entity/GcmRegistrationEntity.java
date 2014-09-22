package me.lazerka.mf.entity;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.*;
import org.joda.time.DateTime;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dzmitry Lazerka
 *
 */
@Entity
public class GcmRegistrationEntity {
	@Parent
	Ref<MfUser> user;

	@Id
	String sha256;

	@Index
	DateTime createdDate;

	@Index
	/** Aka Registration ID */
	String token;

	@Index
	private int appVersion;

	private GcmRegistrationEntity() {
	}

	public GcmRegistrationEntity(
			@Nonnull MfUser user,
			@Nonnull String token,
			@Nonnull DateTime createdDate
	) {
		this.user = Ref.create(user);
		this.token = checkNotNull(token);
		this.createdDate = createdDate;

		sha256 = Hashing.sha256().hashString(token, Charsets.UTF_8).toString();
	}

	@OnSave
	private void onSave() {
		// Prevent saving accounts without existing KiUser.
		if (!user.isLoaded()) {
			user.safe();
		}
	}

	public MfUser getUser() {
		return user.get();
	}

	public String getId() {
		return sha256;
	}

	/** Long string, aka token. */
	public String getToken() {
		return token;
	}

	public DateTime getCreatedDate() {
		return createdDate;
	}

	public void setAppVersion(int appVersion) {
		this.appVersion = appVersion;
	}

	public int getAppVersion() {
		return appVersion;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		GcmRegistrationEntity that = (GcmRegistrationEntity) o;

		if (!sha256.equals(that.sha256)) return false;
		if (!user.equals(that.user)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = user.hashCode();
		result = 31 * result + sha256.hashCode();
		return result;
	}
}
