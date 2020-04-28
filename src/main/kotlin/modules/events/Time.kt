package modules.events

/** Stores time in milliseconds
 *  @throws IllegalArgumentException on invalid time
 */
class Time(hour: Int, minute: Int) {

    val time : Long

    init {
        require(hour in 0..23 && minute in 0..59)
        time = (hour * 60 + minute) * 60 * 1000L
    }
}