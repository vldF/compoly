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
 * Returns amount of days between 2 dates.
 * Returns zero if days are the same.
 * Returns negative number if first day is after second day.
 */
fun daysUntil(firstDay: Date, secondDay: Date): Long {
    val diff: Long = secondDay.time - firstDay.time
    return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)
}