package me.lazerka.mf.gae.entity;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;
import org.joda.time.DateTime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * That's generated by GCM.
 *
 * @author Dzmitry Lazerka
 */
@Entity
@Cache
public class GcmRegistrationEntity {
	@Parent
	private Ref<MfUser> user;

	@Id
	private String id;

	private DateTime createdDate;

	@Nullable
	private Integer appVersion;

	private GcmRegistrationEntity() {}

	public GcmRegistrationEntity(
			@Nonnull MfUser user,
			@Nonnull String id,
			@Nonnull DateTime createdDate,
			@Nullable Integer appVersion
	) {
		this.user = Ref.create(user);
		this.id = checkNotNull(id);
		this.createdDate = checkNotNull(createdDate);
		this.appVersion = appVersion;
	}

	public GcmRegistrationEntity(@Nonnull MfUser user, String id, DateTime createdDate) {
		this(user, id, createdDate, null);
	}

	public Ref<MfUser> getUser() {
		return user;
	}

	/** Long string, aka id. */
	public String getId() {
		return id;
	}

	public DateTime getCreatedDate() {
		return createdDate;
	}

	public void setAppVersion(int appVersion) {
		this.appVersion = appVersion;
	}

	@Nullable
	public Integer getAppVersion() {
		return appVersion;
	}

	public static Key<GcmRegistrationEntity> key(MfUser user, String id) {
		return Key.create(user.key(), GcmRegistrationEntity.class, id);
	}
}
