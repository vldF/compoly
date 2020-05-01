package modules.events

import io.github.classgraph.ClassGraph
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import log
import modules.millisecondInDay
import modules.timeZone
import kotlin.concurrent.thread

class EventStream : Runnable {

    private val events: List<Event>

    init {
        log.info("Initialising EventStream...")

        var loaded: List<Event> = emptyList()
        ClassGraph().enableAllInfo().whitelistPackages("modules.events")
            .scan().use { scanResult ->
                val filtered = scanResult.getClassesImplementing("modules.events.Event")
                    .filter { classInfo ->
                        classInfo.hasAnnotation("modules.Active")
                    }
                loaded = filtered
                    .map { it.loadClass() }
                    .map { it.getConstructor().newInstance() } as List<Event>
            }

        events = loaded.filter { it.schedule.isNotEmpty() }

        log.info("Events: ${events.map { it.javaClass.toString() + ":" + it.schedule.toString() } }")
        log.info("EventStream is initialised")
    }

    private fun calculateDelayTime(eventTime: Long, currentTime: Long) =
            if (eventTime > currentTime) {
                eventTime - currentTime
            } else {
                millisecondInDay - (currentTime - eventTime)
            }

    override fun run() {
        thread {
            log.info("EventStream is running...")
            runBlocking {
                val jobs = mutableListOf<Job>()
                for (event in events) {
                    val job = launch {

                        val schedule = event.schedule

                        var localTime = System.currentTimeMillis() + timeZone
                        var timeSinceDayStart = localTime % (millisecondInDay)

                        var i = 0
                        for (call in schedule.indices) {
                            if (timeSinceDayStart < schedule[call].time) {
                                i = call
                                break
                            }
                        }

                        var time = calculateDelayTime(schedule[i].time, timeSinceDayStart)
                        log.info("Sleeping for $time until next <${event.name}> call")
                        delay(time)

                        while (true) {
                            log.info("Calling <${event.name}>")
                            event.call()

                            if (++i == schedule.size) {
                                i = 0
                            }

                            localTime = System.currentTimeMillis() + timeZone
                            timeSinceDayStart = localTime % (millisecondInDay)
                            time = calculateDelayTime(schedule[i].time, timeSinceDayStart)
                            log.info("Sleeping for $time until next <${event.name}> call")
                            delay(time)
                        }
                    }
                    jobs.add(job)
                }
            }
        }
    }
}