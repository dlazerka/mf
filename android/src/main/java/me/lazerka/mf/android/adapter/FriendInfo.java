package me.lazerka.mf.android.adapter;

import android.os.Bundle;
import android.support.annotation.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Bean for holding emails together with contact info.
 *
 * Shallow-immutable.
 *
 * @author Dzmitry Lazerka
 */
public class FriendInfo {
	public final long id;

	@Nonnull
	public final String lookupKey;

	@Nonnull
	public final String displayName;

	@Nullable
	public final String photoUri;

	@Nonnull
	public final Set<String> emails = new HashSet<>();

	public static FriendInfo fromBundle(Bundle bundle) {
		long id = checkNotNull(bundle.getLong("id"));
		String lookupKey = checkNotNull(bundle.getString("lookupKey"));
		String displayName = checkNotNull(bundle.getString("displayName"));
		ArrayList<String> emails = checkNotNull(bundle.getStringArrayList("emails"));

		return new FriendInfo(
				id,
				lookupKey,
				displayName,
				bundle.getString("photoUri"),
				emails
		);
	}

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

	public Bundle toBundle() {
		Bundle result = new Bundle();

		result.putLong("id", id);
		result.putString("lookupKey", lookupKey);
		result.putString("displayName", displayName);
		result.putString("photoUri", photoUri);
		result.putStringArrayList("emails", new ArrayList<>(emails));

		return result;
	}
}
