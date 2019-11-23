import modules.icsEvents.IcsEvents
import modules.Module
import modules.pageChecking.PageChecker
import modules.weather.Weather
import java.util.*
import kotlin.concurrent.timer

fun main() {
    log.info("Starting")

    val modules = listOf(IcsEvents(), PageChecker(), Weather())
    val timedModules = mutableListOf<Module>()
    val periodicalModules = mutableListOf<Module>()

    for (module in modules) {
        when {
            module.callingType == 0 -> timedModules.add(module)
            module.callingType == 1 -> periodicalModules.add(module)
            else -> {
                log.warning("${module.name} wrong! callingType is not valid")
            }
        }
    }
    log.info("${modules.size} linked")
    log.info("Initialization done")

    for (module in periodicalModules) {
        module.millis.forEach {
            timer("main loop", false, it, period=200L) {
                module.call()
            }
        }
    }

    while (true) {
        for (module in timedModules) {
            val time = System.currentTimeMillis() + 1000L * 60 * 60 * 3
            val currentTimeSinceDayStart = (time / 1000L) % (60 * 60 * 24)
            if (module.millis.any { currentTimeSinceDayStart - it  in 0 until 10 * 60} && time - module.lastCalling > 1000L * 60 * 60) {
                module.lastCalling = time
                module.call()
            }
        }
    }
}