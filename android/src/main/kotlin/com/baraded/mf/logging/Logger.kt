
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

package com.baraded.mf.logging

import android.util.Log
import com.google.firebase.crash.FirebaseCrash
import org.slf4j.helpers.MessageFormatter

class Logger(val name: String) {

	fun trace(format: String, vararg args: Any) = formatAndLog(Log.VERBOSE, format, *args)

	fun debug(format: String, vararg args: Any) = formatAndLog(Log.DEBUG, format, *args)

	fun info(format: String, vararg args: Any) = formatAndLog(Log.INFO, format, *args)

	fun warn(msg: String) = log(Log.WARN, msg, null)

	fun warn(format: String, vararg args: Any) = formatAndLog(Log.WARN, format, *args)

	fun warn(msg: String, t: Throwable) {
		log(Log.WARN, msg, t)

		FirebaseCrash.report(t)
	}

	fun error(msg: String) = log(Log.ERROR, msg, null)

	fun error(format: String, vararg args: Any) = formatAndLog(Log.ERROR, format, *args)

	fun error(msg: String, t: Throwable) {
		log(Log.ERROR, msg, t)

		FirebaseCrash.report(t)
	}

	fun error(t: Throwable) {
		log(Log.ERROR, t.message ?: t.javaClass.simpleName, t)

		FirebaseCrash.report(t)
	}

	private fun formatAndLog(priority: Int, format: String, vararg argArray: Any) {
		if (isLoggable(priority)) {
			val ft = MessageFormatter.arrayFormat(format, argArray)
			logInternal(priority, ft.message, ft.throwable)
		}
	}

	private fun log(priority: Int, message: String, throwable: Throwable?) {
		if (isLoggable(priority)) {
			logInternal(priority, message, throwable)
		}
	}

	private fun isLoggable(priority: Int) = Log.isLoggable(name, priority)

	private fun shouldReport(priority: Int) = (priority == Log.WARN || priority == Log.ERROR)

	private fun logInternal(priority: Int, message: String, throwable: Throwable?) {
		var message2 = message
		if (throwable != null) {
			message2 += '\n' + Log.getStackTraceString(throwable)
		}
		Log.println(priority, name, message2)

		// If throwable not null we will report it above.
		if (throwable == null && shouldReport(priority)) {
			FirebaseCrash.report(Exception(name + ": " + message2))
		}
	}
}
