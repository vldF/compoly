package modules.events

import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Stores time in milliseconds
 * @throws IllegalArgumentException on invalid time
 */
class Time(hour: Int, minute: Int) {

    val time : Long

    init {
        require(hour in 0..23 && minute in 0..59)
        time = (hour * 60 + minute) * 60 * 1000L
    }
}

/**
 * returns amount of days between 2 dates. May be negative.
 */
fun daysUntil(day1: Date, day2: Date): Long {
    val diff: Long = day2.time - day1.time
    return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)
}