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

package com.baraded.mf;

import android.os.SystemClock;
import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;

import java.util.concurrent.TimeUnit;

/**
 * Stopwatch that counts realtime (including time spent in deep sleep).
 */
public class Sw {

	private final Stopwatch stopwatch;

	public Sw(Ticker ticker) {
		stopwatch = Stopwatch.createStarted(ticker);
	}

	public static Sw realtime() {
		return new Sw(REALTIME_TICKER);
	}

	public static Sw uptime() {
		return new Sw(UPTIME_TICKER);
	}

	public static Sw thread() {
		return new Sw(CURRENT_THREAD_TICKER);
	}

	private static final Ticker REALTIME_TICKER = new Ticker() {
		@Override
		public long read() {
			return SystemClock.elapsedRealtimeNanos();
		}
	};

	private static final Ticker UPTIME_TICKER = new Ticker() {
		@Override
		public long read() {
			return SystemClock.uptimeMillis() * 1000_000;
		}
	};

	private static final Ticker CURRENT_THREAD_TICKER = new Ticker() {
		@Override
		public long read() {
			return SystemClock.currentThreadTimeMillis() * 1000_000;
		}
	};

	public long h() {
		return stopwatch.elapsed(TimeUnit.HOURS);
	}

	public long s() {
		return stopwatch.elapsed(TimeUnit.SECONDS);
	}

	public long ms() {
		return stopwatch.elapsed(TimeUnit.MILLISECONDS);
	}

	public long mk() {
		return stopwatch.elapsed(TimeUnit.MICROSECONDS);
	}

	public long ns() {
		return stopwatch.elapsed(TimeUnit.NANOSECONDS);
	}

	public Sw start() {
		stopwatch.start();
		return this;
	}

	public Sw stop() {
		stopwatch.stop();
		return this;
	}

	public Sw reset() {
		stopwatch.reset();
		return this;
	}

	public Sw restart() {
		stopwatch.reset();
		stopwatch.start();
		return this;
	}
}
