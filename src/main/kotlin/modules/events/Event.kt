package modules.events

/**
 * Interface for events that will be called on schedule during a day.
 * @see ActiveEvent to make it runnable.
 */
interface Event {
    val name: String // Will be used in logging
    val schedule: List<Time> // Time of the day it will be called
    fun call()
}