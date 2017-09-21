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
