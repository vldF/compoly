package modules.events

import io.github.classgraph.ClassGraph
import log

class EventStream : Thread() {

    private data class Pass(val event: Event, val time: Long) //time in milliseconds

    private val schedule: List<Pass>

    init {
        log.info("Initialising EventStream...")

        var events: List<Event> = emptyList()
        ClassGraph().enableAllInfo().whitelistPackages("modules.events")
            .scan().use { scanResult ->
                val filtered = scanResult.getClassesImplementing("modules.events.Event")
                    .filter { classInfo ->
                        classInfo.hasAnnotation("modules.events.ActiveEvent")
                    }
                events = filtered
                    .map { it.loadClass() }
                    .map { it.getConstructor().newInstance() } as List<Event>
            }

        val schedule = mutableListOf<Pass>()
        events.map {
            event ->
            schedule.addAll(
                event.schedule.map { time -> Pass(event, time.time) }
            )
        }
        schedule.sortBy { it.time }
        this.schedule = schedule
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