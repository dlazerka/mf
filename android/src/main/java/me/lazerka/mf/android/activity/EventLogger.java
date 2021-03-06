/*
 *     Find Us: privacy oriented location tracker for your friends and family.
 *     Copyright (C) 2016 Dzmitry Lazerka dlazerka@gmail.com
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

package me.lazerka.mf.android.activity;

import android.os.Bundle;
import com.google.firebase.analytics.FirebaseAnalytics;

import javax.annotation.Nullable;

public class EventLogger {
	private final String eventName;
	@Nullable // in error case
	private final FirebaseAnalytics firebaseAnalytics;
	private final Bundle bundle = new Bundle();

	public EventLogger(String eventName, @Nullable FirebaseAnalytics firebaseAnalytics) {
		this.eventName = eventName;
		this.firebaseAnalytics = firebaseAnalytics;
	}

	public EventLogger param(String key, String value) {
		bundle.putString(key, value);
		return this;
	}

	public EventLogger param(String key, int value) {
		bundle.putInt(key, value);
		return this;
	}

	public EventLogger param(String key, long value) {
		bundle.putLong(key, value);
		return this;
	}

	public EventLogger param(String key, boolean value) {
		bundle.putBoolean(key, value);
		return this;
	}

	public void send() {
		if (firebaseAnalytics != null) {
			firebaseAnalytics.logEvent(eventName, bundle);
		}
	}
}
