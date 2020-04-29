package modules.events.icsEvents

import api.Vk
import chatIds
import modules.Active
import modules.events.Event
import modules.events.Time
import modules.events.daysUntil
import java.text.SimpleDateFormat
import java.util.*

@Active
class IcsEvents : Event {

    override val name = "Checks icsEvents from the calendar"
    override val schedule = listOf(Time(8, 0))

    private val formatter = SimpleDateFormat("EEEE, d MMMM yyyy")
    private val scope = 5 //days

    override fun call() {
        val reader = Reader()
        val currentTime = Date()

        val currentEvents = mutableListOf<String>()
        val nextEvents = mutableListOf<String>()

        for (localEvent in reader.read()) {
            val info = "[${localEvent.category}] ${localEvent.name}:\n${formatter.format(localEvent.dateStart)}\n"
            val daysUntil = daysUntil(currentTime, Date(localEvent.dateEnd))
            if (daysUntil == 0L) {
                currentEvents.add(info)
            } else if (daysUntil <= scope) {
                nextEvents.add(info)
            }
        }

        val msg = StringBuilder("\uD83D\uDDD3Ближайшие/недавние:\n")
        if (currentEvents.isEmpty())
            msg.append("Ничего\n")
        else
            msg.append(currentEvents.joinToString(separator = "\n"))

        msg.append("\n\uD83D\uDDD3$scope дней:\n")
        if (nextEvents.isEmpty())
            msg.append("Пусто\n")
        else
            msg.append(nextEvents.joinToString(separator = "\n"))

        Vk().send(msg.toString(), chatIds)
    }
}