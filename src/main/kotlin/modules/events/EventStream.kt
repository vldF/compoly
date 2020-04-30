package modules.events

import io.github.classgraph.ClassGraph
import log
import modules.millisecondInDay
import modules.minute
import modules.timeZone
import java.lang.Thread.sleep
import kotlin.concurrent.thread

class EventStream : Runnable {

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
        log.info("Events: ${schedule.map { it.event.javaClass.toString() + ":" + it.time.toString() } }")
        log.info("EventStream is initialised")
    }

    override fun run() {
        thread {
            log.info("EventStream is running...")
            if (schedule.isNotEmpty()) {

                val delta = 5 * minute
                var isActive = true
                var i = 0

                var pass = schedule.first()
                while (true) {
                    val localTime = System.currentTimeMillis() + timeZone
                    val timeSinceDayStart = localTime % (millisecondInDay)
                    if (isActive) {
                        while (timeSinceDayStart > pass.time) {
                            if (timeSinceDayStart - pass.time < delta) {
                                log.info("EventStream: Time is $timeSinceDayStart; Event time is ${pass.time}; Calling <${pass.event.name}>")
                                pass.event.call()
                            } else {
                                log.info("EventStream: Outdated Event <${pass.event.name}>")
                            }
                            i++
                            if (i >= schedule.size) {
                                i = 0
                                log.info("EventStream: End of schedule")
                                isActive = false
                                break
                            }
                            pass = schedule[i]
                            log.info("EventStream: Next event is <${pass.event.name}>")
                        }
                    } else {
                        if (timeSinceDayStart < delta) {
                            log.info("EventStream: Start of schedule")
                            isActive = true
                        }
                    }
                    sleep(minute)
                }
            }
        }
    }
}