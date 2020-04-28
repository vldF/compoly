package modules.events

import log
import org.reflections.Reflections

class EventStream : Runnable {

    private val schedule: MutableList<Pair<Event, Time>> = mutableListOf()

    init {
        log.info("Initialising EventStream...")

        @Suppress("UNCHECKED_CAST")
        val events = Reflections("modules.events").getTypesAnnotatedWith(ActiveEvent::class.java)
            .filter { it.superclass is Event }
            .map {
                it.getDeclaredConstructor().newInstance()
            } as List<Event>

        events.map {
            event ->
            schedule.addAll(
                event.schedule.map { time -> Pair(event, time) }
            )
        }
        schedule.sortBy { it.second.time }
        if (schedule.isNotEmpty()) {

        }
    }

    override fun run() {
        while (true) {
        }
    }
}