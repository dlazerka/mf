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

package com.baraded.mf.logging;

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import java.util.regex.Pattern


class EventLogger(
        private val eventName: String,
        private val firebaseAnalytics: FirebaseAnalytics
) {
    private val bundle = Bundle()

    init {
        checkName(eventName)
    }

    fun param(key: String, value: String): EventLogger {
        checkKey(key)
        bundle.putString(key, value)
        return this
    }

    fun param(key: String, value: Int): EventLogger {
        checkKey(key)
        bundle.putInt(key, value)
        return this
    }

    fun param(key: String, value: Long): EventLogger {
        checkKey(key)
        bundle.putLong(key, value)
        return this
    }

    fun param(key: String, value: Boolean): EventLogger {
        checkKey(key)
        bundle.putBoolean(key, value)
        return this
    }

    fun send() {
        // It logs the event to Logcat as well.
        firebaseAnalytics.logEvent(eventName, bundle)
    }

    companion object {
        /**
         * Otherwise FirebaseCrash errors "Name must consist of letters, digits or _ (underscores)."
         * for both event name and param name.
         */
        private val NAME = Pattern.compile("^[a-z0-9_]{1,32}$", Pattern.CASE_INSENSITIVE)
    }

    private fun checkKey(key: String) {
        if (!NAME.matcher(key).matches()) {
            throw IllegalArgumentException("Event key " + key)
        }
    }

    private fun checkName(name: String) {
        if (!NAME.matcher(name).matches()) {
            throw IllegalArgumentException("Event name: " + name)
        }
    }
}