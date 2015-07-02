package me.lazerka.mf.android.adapter;

import android.os.Bundle;
import android.support.annotation.Nullable;
import me.lazerka.mf.api.object.UserInfo;

import javax.annotation.Nonnull;
import java.util.*;

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

	/**
	 * Server-side data about the user.
	 * Key is email (client's version, i.e. not canonicalized).
	 *
	 * Null means not yet received response from server.
	 *
	 * It's totally possible that a contact has multiple emails that correspond to different users on server.
	 * We handle them all.
	 */
	@Nullable
	public Map<String, UserInfo> serverInfos;

	public static FriendInfo fromBundle(Bundle bundle) {
		long id = checkNotNull(bundle.getLong("id"));
		String lookupKey = checkNotNull(bundle.getString("lookupKey"));
		String displayName = checkNotNull(bundle.getString("displayName"));
		ArrayList<String> emails = checkNotNull(bundle.getStringArrayList("emails"));

		Map<String, UserInfo> serverInfos = null;
		Bundle serverInfoBundle = bundle.getBundle("serverInfos");
		if (serverInfoBundle != null) {
			serverInfos = new HashMap<>(serverInfoBundle.size());
			for(String email : serverInfoBundle.keySet()) {
				Bundle userInfoBundle = checkNotNull(serverInfoBundle.getBundle(email));
				String serverEmail = checkNotNull(userInfoBundle.getString("email"));
				serverInfos.put(email, new UserInfo(serverEmail));
			}
		}

		return new FriendInfo(
				id,
				lookupKey,
				displayName,
				bundle.getString("photoUri"),
				emails,
				serverInfos
		);
	}

	public FriendInfo(
			long id,
			@Nonnull String lookupKey,
			@Nonnull String displayName,
			@Nullable String photoUri,
			@Nonnull Collection<String> emails,
			@Nullable Map<String, UserInfo> serverInfos
	) {
		this.id = id;
		this.lookupKey = checkNotNull(lookupKey);
		this.displayName = checkNotNull(displayName);
		this.photoUri = photoUri;
		this.emails.addAll(checkNotNull(emails));
		this.serverInfos = serverInfos;
	}

	public Bundle toBundle() {
		Bundle result = new Bundle();

		result.putLong("id", id);
		result.putString("lookupKey", lookupKey);
		result.putString("displayName", displayName);
		result.putString("photoUri", photoUri);
		result.putStringArrayList("emails", new ArrayList<>(emails));
		if (serverInfos != null) {
			Bundle serverInfosBundle = new Bundle();
			for(String email : serverInfos.keySet()) {
				UserInfo userInfo = serverInfos.get(email);
				Bundle userInfoBundle = new Bundle();
				userInfoBundle.putString("email", userInfo.getEmail());
				serverInfosBundle.putBundle(email, userInfoBundle);
			}

			result.putBundle("serverInfos", serverInfosBundle);
		}

		return result;
	}
}
