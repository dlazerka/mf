package me.lazerka.mf.android;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.support.annotation.WorkerThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Type-safe API for SharedPreferences, so that we have one place for them.
 */
public class Preferences {
	private static final Logger logger = LoggerFactory.getLogger(Preferences.class);

	private final String ACCOUNT_NAME = "account.name";
	private final String ACCOUNT_TYPE = "account.type";

	private final String GAE_AUTH_TOKEN = "gae.auth.token";

	private final String FRIENDS = "mf.friends";
	private final String GCM_APP_VERSION = "gcm.app.version";
	private final String GCM_TOKEN = "gcm.token";
	private final String GCM_TOKEN_SENT = "gcm.token.sent";

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
		List<Uri> result = new ArrayList<>(set.size());
		for(String uriString : set) {
			Uri parsed = Uri.parse(uriString);
			result.add(parsed);
		}
		logger.info("getFriends " + result.size());
		return result;
	}

	public boolean addFriend(Uri contactUri) {
		logger.info("addFriend " + contactUri);
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
		logger.info("removeFriend " + contactUri);
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
				logger.info("GCM Registration ID not found.");
				return null;
			} else {
				// Do not log real registration ID as it should be private.
				logger.info("GCM Registration ID found.");
			}

			// Check if app was updated; if so, it must clear the registration ID
			// since the existing regID is not guaranteed to work with the new
			// app version.
			// See build.gradle where it's defined
			int currentVersion = Application.getVersion();
			if (registeredVersion != currentVersion) {
				logger.info("App version changed.");
				return null;
			}

			return result;
		}
	}

	/**
	 * This should not be backed up when user uses Backup/Restore feature.
	 * See MfBackupAgent for that.
	 */
	public void setGcmToken(@Nonnull String gcmToken) {
		logger.info("GCM Registration ID stored.");
		preferences.edit()
				.putString(GCM_TOKEN, gcmToken)
				.putInt(GCM_APP_VERSION, Application.getVersion())
				.apply();
	}

	@SuppressLint("CommitPrefEdits")
	public void clearGcmToken() {
		preferences.edit()
				.remove(GCM_TOKEN)
				.remove(GCM_APP_VERSION)
				.commit();
	}

	/**
	 * This should not be backed up when user uses Backup/Restore feature.
	 * See MfBackupAgent for that.
	 */
	@SuppressLint("CommitPrefEdits")
	@WorkerThread
	public void setGcmTokenSent(@Nonnull String gcmToken) {
		logger.info("setGcmTokenSent()");
		preferences.edit()
				.putString(GCM_TOKEN_SENT, gcmToken)
				.putInt(GCM_APP_VERSION, Application.getVersion())
				.commit();
	}

	/**
	 * This should not be backed up when user uses Backup/Restore feature.
	 * See MfBackupAgent for that.
	 */
	@SuppressLint("CommitPrefEdits")
	@WorkerThread
	public void clearGcmTokenSent(@Nonnull String gcmToken) {
		logger.info("setGcmTokenSent()");
		preferences.edit()
				.remove(GCM_TOKEN_SENT)
				.remove(GCM_APP_VERSION)
				.commit();
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
		logger.info("onBeforeBackup");
		clearGcmToken();
	}
}
