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
import android.content.SharedPreferences;
import android.support.annotation.WorkerThread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Type-safe API for SharedPreferences, so that we have one place for them.
 */
public class GcmManager {
	private static final Logger logger = LoggerFactory.getLogger(GcmManager.class);

	private final String GCM_APP_VERSION = "gcm.app.version";
	private final String GCM_TOKEN_SENT_AT = "gcm.token.sent";

	private final SharedPreferences sharedPreferences;

	public GcmManager(SharedPreferences sharedPreferences) {
		// Same file name as RoboGuice default.
		this.sharedPreferences = sharedPreferences;
	}

	/**
	 * This should not be backed up when user uses Backup/Restore feature.
	 * See MfBackupAgent for that.
	 */
	@SuppressLint("CommitPrefEdits")
	@WorkerThread
	public void setGcmTokenSent(String token) {
		logger.info("setGcmTokenSent()");
		sharedPreferences.edit()
				.putLong(GCM_TOKEN_SENT_AT, System.currentTimeMillis())
				.putInt(GCM_APP_VERSION, Application.getVersion())
				.apply();
	}

	/**
	 * This should not be backed up when user uses Backup/Restore feature.
	 * See MfBackupAgent for that.
	 */
	@SuppressLint("CommitPrefEdits")
	@WorkerThread
	public void clearGcmTokenSent() {
		logger.info("setGcmTokenSent()");
		sharedPreferences.edit()
				.remove(GCM_TOKEN_SENT_AT)
				.remove(GCM_APP_VERSION)
				.apply();
	}

	public void onBeforeBackup() {
		logger.info("onBeforeBackup");
		clearGcmTokenSent();
	}
}
