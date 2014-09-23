package me.lazerka.mf.android;

import android.accounts.Account;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.util.Log;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * My API for SharedPreferences, type-safe.
 */
public class Preferences {
	private final String TAG = getClass().getName();

	private final String ACCOUNT_NAME = "account.name";
	private final String ACCOUNT_TYPE = "account.type";

	private final String FRIENDS = "mf.friends";

	private final String GCM_APP_VERSION = "gcm.app.version";
	private final String GCM_REGISTRATION_ID = "gcm.registration.id";
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

	public List<Uri> getFriends() {
		Set<String> set = preferences.getStringSet(FRIENDS, Collections.<String>emptySet());
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
	public String getGcmRegistrationId() {
		String result = preferences.getString(GCM_REGISTRATION_ID, null);
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
		int registeredVersion = preferences.getInt(GCM_APP_VERSION, Integer.MIN_VALUE);
		int currentVersion = getAppVersion();
		if (registeredVersion != currentVersion) {
			Log.i(TAG, "App version changed.");
			return null;
		}

		return result;
	}

	/**
	 * This should not be backed up when user uses Backup/Restore feature.
	 * See MfBackupAgent for that.
	 */
	public void setGcmRegistrationId(@Nonnull String gcmRegistrationId) {
		Log.v(TAG, "GCM Registration ID stored.");
		preferences.edit()
				.putString(GCM_REGISTRATION_ID, gcmRegistrationId)
				.putInt(GCM_APP_VERSION, getAppVersion())
				// TODO: storedOnServer
				.apply();
	}

	/**
	 * This should not be backed up when user uses Backup/Restore feature.
	 * See MfBackupAgent for that.
	 * @param gcmRegistrationId to compare with current and avoid race conditions.
	 * @return is current token equals given.
	 */
	public boolean setGcmRegistrationServerKnows(@Nonnull String gcmRegistrationId) {
		Log.v(TAG, "GCM Registration ID stored.");
		synchronized (preferences) {
			String currentId = preferences.getString(GCM_REGISTRATION_ID, null);
			if (!gcmRegistrationId.equals(currentId)) {
				return false;
			}
			preferences.edit()
					.putBoolean(GCM_SERVER_KNOWS, true)
					.apply();
			return true;
		}
	}

	public int getGcmAppVersion() {
		return preferences.getInt(GCM_APP_VERSION, Integer.MIN_VALUE);
	}

	/**
	 * @return Application's version code from the {@code PackageManager}.
	 */
	private static int getAppVersion() {
		String packageName = Application.context.getPackageName();
		PackageManager packageManager = Application.context.getPackageManager();
		try {
			PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
			return packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			// should never happen
			throw new RuntimeException("Could not get package name: " + e);
		}
	}

	public void onBeforeBackup() {
		Log.v(TAG, "onBeforeBackup");
		preferences.edit()
				.remove(GCM_REGISTRATION_ID)
				.apply();
	}

}
