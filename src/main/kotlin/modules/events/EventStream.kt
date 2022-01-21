package modules.events

import getDayTime
import io.github.classgraph.ClassGraph
import kotlinx.coroutines.*
import log
import millisecondInDay
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.Exception
import java.lang.Runnable
import kotlin.concurrent.thread

object EventStream {

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

        log.info("Events: ${events.map { it.javaClass.toString() + ":" + it.schedule.toString() }}")
        log.info("EventStream is initialised")
    }

    fun start() {
        thread {
            log.info("EventStream is running...")
            runBlocking {
                val jobs = mutableListOf<Job>()
                for (event in events) {
                    val job = launch {
                        try {
                            val schedule = event.schedule

                            var callIndex = 0
                            for (call in schedule.indices) {
                                if (getDayTime() < schedule[call].time) {
                                    callIndex = call
                                    break
                                }
                            }

                            var time = calculateDelayTime(schedule[callIndex].time, getDayTime())
                            log.info("Sleeping for $time until next <${event.name}> call")
                            delay(time)

                            while (true) {
                                log.info("Calling <${event.name}>")
                                event.call()

                                if (++callIndex == schedule.size) {
                                    callIndex = 0
                                }

                                time = calculateDelayTime(schedule[callIndex].time, getDayTime())
                                log.info("Sleeping for $time until next <${event.name}> call")
                                delay(time)
                            }
                        } catch (e: Exception) {
                            val sw = StringWriter()
                            val pw = PrintWriter(sw)
                            e.printStackTrace(pw)
                            val sStackTrace = sw.toString()
                            log.warning("Exception in <${event.name}>, aborting the event...")
                            log.warning(sStackTrace)
                        } finally {
                            this.cancel()
                        }
                    }
                    jobs.add(job)
                }
            }
        }
    }

    /** For single time scheduled event*/
    fun addDynamicTask(runnable: Runnable) {
        Thread(runnable).start()
    }

    private fun calculateDelayTime(eventTime: Long, currentTime: Long) =
        if (eventTime > currentTime) {
            eventTime - currentTime
        } else {
            millisecondInDay - (currentTime - eventTime)
        }
}