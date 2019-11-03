import modules.IcsEvents.IcsEvents
import modules.Module
import kotlin.concurrent.timer

fun main() {
    val modules = listOf<Module>(IcsEvents())
    timer("main loop", false, 0L, 10*1000L) {
        for (module in modules) {
            module.call()
        }
    }
}