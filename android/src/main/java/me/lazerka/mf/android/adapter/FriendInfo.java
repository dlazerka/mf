package me.lazerka.mf.android.adapter;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.api.object.UserInfo;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
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
	@JsonProperty
	public long id;

	@JsonProperty
	public String lookupKey;

	@JsonProperty
	public String displayName;

	@JsonProperty
	@Nullable
	public String photoUri;

	@JsonProperty
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

	static {
		AsyncTask.execute(
				new Runnable() {
					@Override
					public void run() {
					}
				});
	}

	/**
	 * Deserializes an instance from a Bundle, created previously by {@link #toBundle()}.
	 * Uses Jackson for simplicity and easier maintenance.
	 */
	public static FriendInfo fromBundle(Bundle bundle) {
		String json = checkNotNull(bundle.getString("json"));
		try {
			return Application.jsonMapper.readValue(json, FriendInfo.class);
		} catch (IOException e) {
			throw new RuntimeException("Error deserializing", e);
		}
	}

	// For Jackson
	private FriendInfo() {}

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

	/**
	 * Serializes this to a Bundle, to be read later by {@link #fromBundle(Bundle)}.
	 * Uses Jackson for simplicity and easier maintenance.
	 */
	public Bundle toBundle() {
		try {
			String json = Application.jsonMapper.writeValueAsString(this);

			Bundle result = new Bundle();
			result.putString("json", json);
			return result;
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Error serializing", e);
		}
	}

}
