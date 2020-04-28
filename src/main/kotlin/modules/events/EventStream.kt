package modules.events

import io.github.classgraph.ClassGraph
import log


class EventStream : Thread() {

    private data class Pass(val event: Event, val time: Long) //time in milliseconds

    private val schedule: MutableList<Pass> = mutableListOf()

    init {
        log.info("Initialising EventStream...")

        val events: List<Event> = TODO()

        events.map {
            event ->
            schedule.addAll(
                event.schedule.map { time -> Pass(event, time.time) }
            )
        }
        schedule.sortBy { it.time }
    }

    override fun run() {
        if (schedule.isNotEmpty()) {
            var i = 0
            var pass = schedule.first()
            while (true) {
                val localTime = System.currentTimeMillis() + 1000L * 60 * 60 * 3 //Moscow timezone
                val timeSinceDayStart = localTime % (60 * 60 * 24)
                if (timeSinceDayStart > pass.time) {
                    pass.event.call()
                    i++
                    if (i >= schedule.size) {
                        i = 0
                    }
                    pass = schedule[i]
                }
                sleep(60 * 1000L)
            }
        }
    }
}