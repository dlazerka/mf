/*
 *     Copyright (C) 2017 Dzmitry Lazerka
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package me.lazerka.mf.android.activity;

import android.os.Bundle;
import com.google.firebase.analytics.FirebaseAnalytics;

import javax.annotation.Nullable;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

public class EventLogger {
	private final String eventName;
	@Nullable // in error case
	private final FirebaseAnalytics firebaseAnalytics;
	private final Bundle bundle = new Bundle();

	/**
	 * Otherwise FirebaseCrash errors "Name must consist of letters, digits or _ (underscores)."
	 * for both event name and param name.
	 */
	private static final Pattern NAME = Pattern.compile("^[a-z0-9_]{1,32}$", CASE_INSENSITIVE);
	private static void checkKey(String key) {
		if (!NAME.matcher(key).matches()) {
			throw new IllegalArgumentException("Event key " + key);
		}
	}

	private static void checkName(String name) {
		if (!NAME.matcher(name).matches()) {
			throw new IllegalArgumentException("Event name: " + name);
		}
	}

	public EventLogger(String eventName, @Nullable FirebaseAnalytics firebaseAnalytics) {
		checkName(eventName);
		this.eventName = eventName;
		this.firebaseAnalytics = firebaseAnalytics;
	}

	public EventLogger param(String key, String value) {
		checkKey(key);
		bundle.putString(key, value);
		return this;
	}

	public EventLogger param(String key, int value) {
		checkKey(key);
		bundle.putInt(key, value);
		return this;
	}

	public EventLogger param(String key, long value) {
		checkKey(key);
		bundle.putLong(key, value);
		return this;
	}

	public EventLogger param(String key, boolean value) {
		checkKey(key);
		bundle.putBoolean(key, value);
		return this;
	}

	public void send() {
		if (firebaseAnalytics != null) {
			firebaseAnalytics.logEvent(eventName, bundle);
		}
	}
}
