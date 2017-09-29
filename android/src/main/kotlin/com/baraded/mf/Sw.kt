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
import me.lazerka.mf.android.Util.checkState
import java.util.concurrent.TimeUnit

/**
 * Stopwatch that counts realtime (including time spent in deep sleep).
 */
class Sw private constructor(private val ticker: Ticker) {

    companion object {
        @JvmStatic
        fun realtime() = Sw(RealtimeTicker)

        @JvmStatic
        fun uptime() = Sw(UptimeTicker)

        @JvmStatic
        fun thread() = Sw(CurrentThreadTicker)

        @JvmStatic
        fun system() = Sw(SystemTicker)
    }

    private interface Ticker {
        fun read(): Long
    }

    private object RealtimeTicker : Ticker {
        override fun read() = SystemClock.elapsedRealtimeNanos()
    }

    private object UptimeTicker : Ticker {
        override fun read() = SystemClock.uptimeMillis() * 1000_000
    }

    private object CurrentThreadTicker : Ticker {
        override fun read() = SystemClock.currentThreadTimeMillis() * 1000_000
    }

    private object SystemTicker : Ticker {
        override fun read(): Long {
            return System.nanoTime();
        }
    }

    private var isRunning: Boolean = false
    private var elapsedNanos: Long = 0
    private var startTick: Long = 0

    private fun elapsed(timeUnit: TimeUnit): Long {
        return timeUnit.convert(elapsedNanos(), TimeUnit.NANOSECONDS);
    }

    private fun elapsedNanos(): Long {
        if (isRunning) {
            return ticker.read() - startTick + elapsedNanos
        } else {
            return elapsedNanos
        }
    }

    fun h() = elapsed(TimeUnit.HOURS)

    fun m() = elapsed(TimeUnit.MINUTES)

    fun s() = elapsed(TimeUnit.SECONDS)

    fun ms() = elapsed(TimeUnit.MILLISECONDS)

    fun mk() = elapsed(TimeUnit.MICROSECONDS)

    fun ns() = elapsed(TimeUnit.NANOSECONDS)

    fun start(): Sw {
        checkState(!isRunning, "This stopwatch is already running.");
        isRunning = true;
        startTick = ticker.read();
        return this
    }

    fun stop(): Sw {
        val tick = ticker.read()
        checkState(isRunning, "This stopwatch is already stopped.")
        isRunning = false
        elapsedNanos += tick - startTick
        return this
    }

    fun reset(): Sw {
        elapsedNanos = 0;
        isRunning = false;
        return this
    }

    fun restart(): Sw {
        reset()
        start()
        return this
    }
}
