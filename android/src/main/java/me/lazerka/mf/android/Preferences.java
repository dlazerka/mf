/*
 *     Find Us: privacy oriented location tracker for your friends and family.
 *     Copyright (C) 2015 Dzmitry Lazerka dlazerka@gmail.com
 *
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package me.lazerka.mf.android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.WorkerThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Type-safe API for SharedPreferences, so that we have one place for them.
 */
public class Preferences {
	private static final Logger logger = LoggerFactory.getLogger(Preferences.class);

	private final String ACCOUNT_NAME = "account.name";
	private final String ACCOUNT_TYPE = "account.type";

	private final String FRIENDS = "mf.friends";
	private final String GCM_APP_VERSION = "gcm.app.version";
	private final String GCM_TOKEN_SENT_AT = "gcm.token.sent";

	private final SharedPreferences preferences;

	public Preferences(Application app) {
		// Same file name as RoboGuice default.
		preferences = app.getSharedPreferences("default.xml", Context.MODE_PRIVATE);
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
		logger.trace("getFriends " + result.size());
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

	/**
	 * This should not be backed up when user uses Backup/Restore feature.
	 * See MfBackupAgent for that.
	 * @param token
	 */
	@SuppressLint("CommitPrefEdits")
	@WorkerThread
	public void setGcmTokenSent(String token) {
		logger.info("setGcmTokenSent()");
		preferences.edit()
				.putLong(GCM_TOKEN_SENT_AT, System.currentTimeMillis())
				.putInt(GCM_APP_VERSION, Application.getVersion())
				.commit();
	}

	/**
	 * This should not be backed up when user uses Backup/Restore feature.
	 * See MfBackupAgent for that.
	 */
	@SuppressLint("CommitPrefEdits")
	@WorkerThread
	public void clearGcmTokenSent() {
		logger.info("setGcmTokenSent()");
		preferences.edit()
				.remove(GCM_TOKEN_SENT_AT)
				.remove(GCM_APP_VERSION)
				.commit();
	}


	public void onBeforeBackup() {
		logger.info("onBeforeBackup");
		clearGcmTokenSent();
	}
}
