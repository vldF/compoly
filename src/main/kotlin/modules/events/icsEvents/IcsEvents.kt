package modules.events.icsEvents

import api.Vk
import chatIds
import modules.Active
import modules.events.Event
import modules.events.Time
import java.text.SimpleDateFormat
import kotlin.math.abs

@Active
class IcsEvents : Event {

    override val name = "Check icsEvents from schedule"
    override val schedule = listOf(Time(8, 0))

    private val formatter = SimpleDateFormat("EEEE, d MMMM yyyy")

    override fun call() {
        val reader = Reader()
        val currentTime = System.currentTimeMillis()

        val currentEvents = mutableListOf<String>()
        val nextEvents = mutableListOf<String>()

        for (localEvent in reader.read()) {
            val info = "[${localEvent.category}] ${localEvent.name}:\n${formatter.format(localEvent.dateStart)}\n"
            // todo: refactor next code
            (if (abs(currentTime / (1000 * 60 * 60 * 24) - localEvent.dateStart / (1000 * 60 * 60 * 24)) <= 1L) currentEvents else if (localEvent.dateStart - currentTime in 0..5 * 24 * 60 * 60 * 1000) nextEvents else mutableListOf()).add(
                info
            )
        }

        val msg = StringBuilder("\uD83D\uDDD3Ближайшие/недавние:\n")
        if (currentEvents.isEmpty()) msg.append("Ничего\n")
        msg.append(currentEvents.joinToString(separator = "\n"))
        msg.append("\n\uD83D\uDDD35 дней:\n")
        if (nextEvents.isEmpty()) msg.append("Пусто\n")
        msg.append(nextEvents.joinToString(separator = "\n"))
        Vk().send(msg.toString(), chatIds)
    }
}