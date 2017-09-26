
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

import android.util.Log;
import com.google.firebase.crash.FirebaseCrash;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

public class Logger {

	private final String name;

	public Logger(String name) {
		this.name = name;
	}

	public void trace(String format, Object... args) {
		formatAndLog(Log.VERBOSE, format, args);
	}

	public void debug(String format, Object... args) {
		formatAndLog(Log.DEBUG, format, args);
	}

	public void info(String format, Object... args) {
		formatAndLog(Log.INFO, format, args);
	}

	public void warn(String msg) { log(Log.WARN, msg, null);}

	public void warn(String format, Object... args) { formatAndLog(Log.WARN, format, args);}

	public void warn(String msg, Throwable t) {
		log(Log.WARN, msg, t);

		FirebaseCrash.report(t);
	}

	public void error(String msg) { log(Log.ERROR, msg, null);}

	public void error(String format, Object... args) { formatAndLog(Log.ERROR, format, args);}

	public void error(String msg, Throwable t) {
		log(Log.ERROR, msg, t);

		FirebaseCrash.report(t);
	}

	public void error(Throwable t) {
		log(Log.ERROR, t.getMessage() == null ? null : t.getClass().getSimpleName(), t);

		FirebaseCrash.report(t);
	}

	private void formatAndLog(int priority, String format, Object... argArray) {
		if (isLoggable(priority)) {
			FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
			logInternal(priority, ft.getMessage(), ft.getThrowable());
		}
	}

	private void log(int priority, String message, Throwable throwable) {
		if (isLoggable(priority)) {
			logInternal(priority, message, throwable);
		}
	}

	private boolean isLoggable(int priority) {
		return Log.isLoggable(name, priority);
	}

	private boolean shouldReport(int priority) {
	  return priority == Log.WARN || priority == Log.ERROR;
	}

	private void logInternal(int priority, String message, Throwable throwable) {
		if (throwable != null) {
			message += '\n' + Log.getStackTraceString(throwable);
		}
		Log.println(priority, name, message);

		// If throwable not null we will report it above.
		if (throwable == null && shouldReport(priority)) {
			FirebaseCrash.report(new Exception(name + ": " + message));
		}
	}
}
