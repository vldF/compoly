import modules.icsEvents.icsEvents
import kotlin.concurrent.timer

fun main() {
    val modules = listOf(icsEvents())
    timer("main loop", false, 0L, 10*1000L) {
        for (module in modules) {
            module.call()
        }
    }
}