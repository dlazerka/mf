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

package me.lazerka.mf.android;

import android.os.SystemClock;
import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;

/**
 * For {@link com.google.common.base.Stopwatch}.
 *
 * Counts realtime (including time spent in deep sleep).
 */
public class AndroidTicker extends Ticker {
	private static final AndroidTicker instance = new AndroidTicker();

	public static Stopwatch started() {
		return Stopwatch.createStarted(instance);
	}

	@Override
	public long read() {
		return SystemClock.elapsedRealtimeNanos();
	}
}
