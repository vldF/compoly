import modules.icsEvents.IcsEvents
import modules.Module
import modules.pageChecking.PageChecker
import kotlin.concurrent.timer

fun main() {
    log.info("Starting")

    val modules = listOf(IcsEvents(), PageChecker())
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
        timer("main loop", false, 0L, module.millis) {
            module.call()
        }
    }

    while (true) {
        for (module in modules) {
            val time = System.currentTimeMillis() + 3 * 60 * 60 * 1000L
            val currentTimeSinceDayStart = (time / 1000L) % (60 * 60 * 24)
            if (currentTimeSinceDayStart - module.millis >= 0 && time - module.lastCalling > 10000L) {
                module.lastCalling = time
                module.call()
            }
        }
    }
}