package me.lazerka.mf.entity;

import com.googlecode.objectify.Key;
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
	String id;

	@Index
	DateTime createdDate;

	@Index
	private int appVersion;

	private GcmRegistrationEntity() {
	}

	public static Key<GcmRegistrationEntity> key(MfUser user, String id) {
		return Key.create(user.key(), GcmRegistrationEntity.class, id);
	}

	public GcmRegistrationEntity(
			@Nonnull MfUser user,
			@Nonnull String id,
			@Nonnull DateTime createdDate
	) {
		this.user = Ref.create(user);
		this.id = checkNotNull(id);
		this.createdDate = createdDate;
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

	/** Long string, aka token. */
	public String getId() {
		return id;
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
}
