package modules.loops

import io.github.classgraph.ClassGraph
import kotlinx.coroutines.*
import log
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.concurrent.thread


object LoopStream {

    private val loops: List<Loop>

    init {
        log.info("Initialising LoopStream...")

        ClassGraph()
            .enableAllInfo()
            .whitelistPackages("modules.loops")
            .scan().use { scanResult ->
                val filtered = scanResult.getClassesImplementing("modules.loops.Loop")
                    .filter { classInfo ->
                        classInfo.hasAnnotation("modules.Active")
                    }
                loops = filtered
                    .map { it.loadClass() }
                    .map { it.getConstructor().newInstance() } as List<Loop>
            }

        log.info("Loops: ${loops.map { it.javaClass } }")
        log.info("LoopStream is initialised")
    }

    fun start() {
        thread {
            log.info("LoopStream is running...")
            runBlocking {
                val jobs = mutableListOf<Job>()
                for (loop in loops) {
                    val job = launch {
                        try {
                            while (true) {
                                loop.call()
                                delay(loop.delay)
                            }
                        } catch (e: Exception) {
                            val sw = StringWriter()
                            val pw = PrintWriter(sw)
                            e.printStackTrace(pw)
                            val sStackTrace = sw.toString()
                            log.warning("Exception in <${loop.name}>, aborting the loop...")
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
}