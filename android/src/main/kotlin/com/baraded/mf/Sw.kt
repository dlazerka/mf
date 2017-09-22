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

package com.baraded.mf

import android.os.SystemClock
import com.google.common.base.Stopwatch
import com.google.common.base.Ticker
import java.util.concurrent.TimeUnit

/**
 * Stopwatch that counts realtime (including time spent in deep sleep).
 */
class Sw(ticker: Ticker) {

	companion object {
		@JvmStatic fun realtime() = Sw(RealtimeTicker)
		@JvmStatic fun uptime() = Sw(UptimeTicker)
		@JvmStatic fun thread() = Sw(CurrentThreadTicker)
	}

	private object RealtimeTicker : Ticker() {
		override fun read() = SystemClock.elapsedRealtimeNanos()
	}

	private object UptimeTicker : Ticker() {
		override fun read() = SystemClock.uptimeMillis() * 1000_000
	}

	private object CurrentThreadTicker : Ticker() {
		override fun read() = SystemClock.currentThreadTimeMillis() * 1000_000
	}

	private val stopwatch = Stopwatch.createStarted(ticker)

	fun h() = stopwatch.elapsed(TimeUnit.HOURS)

	fun m() = stopwatch.elapsed(TimeUnit.MINUTES)

	fun s() = stopwatch.elapsed(TimeUnit.SECONDS)

	fun ms() = stopwatch.elapsed(TimeUnit.MILLISECONDS)

	fun mk() = stopwatch.elapsed(TimeUnit.MICROSECONDS)

	fun ns() = stopwatch.elapsed(TimeUnit.NANOSECONDS)

	fun start(): Sw {
		stopwatch.start()
		return this
	}

	fun stop(): Sw {
		stopwatch.stop()
		return this
	}

	fun reset(): Sw {
		stopwatch.reset()
		return this
	}

	fun restart(): Sw {
		stopwatch.reset()
		stopwatch.start()
		return this
	}
}
