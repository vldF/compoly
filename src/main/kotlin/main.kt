import modules.events.EventStream
import modules.loops.LoopStream

fun main() {
    val eventStream = EventStream()
    val loopStream = LoopStream()
    eventStream.run()
    loopStream.run()
}