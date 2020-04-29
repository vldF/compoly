package modules.events

import io.github.classgraph.ClassGraph
import log
import modules.millisecondInDay
import modules.minute
import modules.timeZone

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
                        classInfo.hasAnnotation("modules.Active")
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
        log.info("EventStream is initialised")
    }

    override fun run() {
        if (schedule.isNotEmpty()) {
            val delta = 15 * minute
            var i = 0
            var pass = schedule.first()
            while (true) {
                val localTime = System.currentTimeMillis() + timeZone
                val timeSinceDayStart = localTime % (millisecondInDay)
                while (timeSinceDayStart > pass.time) {
                    if (timeSinceDayStart - pass.time < delta) {
                        log.info("Time is $timeSinceDayStart, calling ${pass.event.name}")
                        pass.event.call()
                    }
                    i++
                    if (i >= schedule.size) {
                        i = 0
                        sleep(millisecondInDay - timeSinceDayStart)
                    }
                    pass = schedule[i]
                }
                sleep(minute)
            }
        }
    }
}