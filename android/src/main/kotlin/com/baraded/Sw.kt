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

package com.baraded

import com.google.common.base.Stopwatch
import java.util.concurrent.TimeUnit

class Sw {
	private val stopwatch = Stopwatch.createStarted()

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
