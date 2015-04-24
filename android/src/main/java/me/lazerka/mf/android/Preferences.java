package me.lazerka.mf.android;

import android.accounts.Account;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.util.Log;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Type-safe API for SharedPreferences, so that we have one place for them.
 */
public class Preferences {
	private final String TAG = getClass().getName();

	private final String ACCOUNT_NAME = "account.name";
	private final String ACCOUNT_TYPE = "account.type";

	private final String GAE_AUTH_TOKEN = "gae.auth.token";

	private final String FRIENDS = "mf.friends";
	private final String GCM_APP_VERSION = "gcm.app.version";
	private final String GCM_TOKEN = "gcm.token";
	private final String GCM_SERVER_KNOWS = "gcm.server.knows";

	private final SharedPreferences preferences;

	public Preferences(Application app) {
		// Same file name as RoboGuice default.
		preferences = app.getSharedPreferences("default.xml", Context.MODE_PRIVATE);
	}

	//@Nullable
	public Account getAccount() {
		String name = preferences.getString(ACCOUNT_NAME, null);
		String type = preferences.getString(ACCOUNT_TYPE, null);
		if (name == null || type == null) {
			return null;
		}
		return new Account(name, type);
	}

	public void setAccount(@Nonnull Account account) {
		Editor editor = preferences.edit();
		editor.putString(ACCOUNT_NAME, account.name);
		editor.putString(ACCOUNT_TYPE, account.type);
		editor.apply();
	}

	public void clearAccount() {
		preferences.edit()
				.remove(ACCOUNT_NAME)
				.remove(ACCOUNT_TYPE)
				.apply();
	}

	@Nonnull
	public List<Uri> getFriends() {
		Set<String> set = preferences.getStringSet(FRIENDS, Collections.<String>emptySet());
		assert set != null;
		List<Uri> result = new ArrayList<>(set.size());
		for(String uriString : set) {
			Uri parsed = Uri.parse(uriString);
			result.add(parsed);
		}
		Log.i(TAG, "getFriends " + result.size());
		return result;
	}

	public boolean addFriend(Uri contactUri) {
		Log.i(TAG, "addFriend " + contactUri);
		synchronized (preferences) {
			// Clone, otherwise value won't be set.
			Set<String> friends = new LinkedHashSet<>(preferences.getStringSet(FRIENDS, new HashSet<String>(1)));
			boolean changed = friends.add(contactUri.toString());
			if (!changed) {
				return false;
			}
			preferences.edit()
					.putStringSet(FRIENDS, friends)
					.apply();
			return true;
		}
	}

	public boolean removeFriend(Uri contactUri) {
		Log.i(TAG, "removeFriend " + contactUri);
		synchronized (preferences) {
			// Clone, otherwise value won't be set.
			Set<String> friends = new LinkedHashSet<>(preferences.getStringSet(FRIENDS, new HashSet<String>(0)));
			boolean changed = friends.remove(contactUri.toString());
			if (!changed) {
				return false;
			}
			preferences.edit()
					.putStringSet(FRIENDS, friends)
					.apply();
			return true;
		}
	}

	@Nullable
	public String getGcmToken() {
		synchronized (preferences) {
			String result = preferences.getString(GCM_TOKEN, null);
			int registeredVersion = preferences.getInt(GCM_APP_VERSION, Integer.MIN_VALUE);

			if (result == null) {
				Log.i(TAG, "GCM Registration ID not found.");
				return null;
			} else {
				// Do not log real registration ID as it should be private.
				Log.v(TAG, "GCM Registration ID found.");
			}

			// Check if app was updated; if so, it must clear the registration ID
			// since the existing regID is not guaranteed to work with the new
			// app version.
			int currentVersion = Application.getVersion();
			if (registeredVersion != currentVersion) {
				Log.i(TAG, "App version changed.");
				return null;
			}

			return result;
		}
	}

	/**
	 * This should not be backed up when user uses Backup/Restore feature.
	 * See MfBackupAgent for that.
	 */
	public void setGcmToken(@Nonnull String gcmRegistrationId) {
		Log.v(TAG, "GCM Registration ID stored.");
		preferences.edit()
				.putString(GCM_TOKEN, gcmRegistrationId)
				.putInt(GCM_APP_VERSION, Application.getVersion())
				.putBoolean(GCM_SERVER_KNOWS, false)
				.apply();
	}

	public boolean getGcmServerKnowsToken(String gcmToken) {
		Map<String, ?> all = preferences.getAll();
		return gcmToken.equals(all.get(GCM_TOKEN)) && (Boolean) all.get(GCM_SERVER_KNOWS);
	}

	/**
	 * This should not be backed up when user uses Backup/Restore feature.
	 * See MfBackupAgent for that.
	 *
	 * @param gcmToken to compare with current and avoid race conditions.
	 * @return is current token equals given.
	 */
	public boolean setGcmServerKnowsToken(@Nonnull String gcmToken) {
		Log.v(TAG, "GCM Registration ID stored.");
		synchronized (preferences) {
			String currentToken = preferences.getString(GCM_TOKEN, null);
			if (!gcmToken.equals(currentToken)) {
				return false;
			}
			preferences.edit()
					.putString(GCM_TOKEN, gcmToken)
					.putBoolean(GCM_SERVER_KNOWS, true)
					.apply();
			return true;
		}
	}

	@Nullable
	public String getGaeAuthToken() {
		return preferences.getString(GAE_AUTH_TOKEN, null);
	}

	public void setGaeAuthToken(@Nonnull String gaeAuthToken) {
		preferences.edit()
				.putString(GAE_AUTH_TOKEN, gaeAuthToken)
				.apply();
	}

	public void onBeforeBackup() {
		Log.v(TAG, "onBeforeBackup");
		preferences.edit()
				.remove(GCM_TOKEN)
				.apply();
	}
}
