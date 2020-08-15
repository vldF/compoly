package modules.events.icsEvents

import api.VkPlatform
import chatIds
import getTime
import modules.events.Event
import modules.events.Time
import modules.events.daysUntil
import java.text.SimpleDateFormat
import java.util.*


class IcsEvents : Event {
    override val name = "ICS calendar checking"
    override val schedule = listOf(Time(8, 0))

    private val formatter = SimpleDateFormat("EEEE, d MMMM yyyy")
    private val scope = 5 //days
    private val vk = VkPlatform

    override fun call() {
        val reader = Reader()
        val currentTime = Date(getTime())

        val currentEvents = mutableListOf<String>()
        val nextEvents = mutableListOf<String>()

        for (localEvent in reader.read()) {
            val info = "[${localEvent.category}] ${localEvent.name}:\n${formatter.format(localEvent.dateStart)}\n"

            val daysUntilStart = daysUntil(currentTime, Date(localEvent.dateStart))
            val daysUntilEnd = daysUntil(currentTime, Date(localEvent.dateEnd))
            if (daysUntilStart <= 0 && daysUntilEnd >= 0) {
                currentEvents.add(info)
            } else if (daysUntilStart in 1..scope) {
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

        chatIds.forEach { vk.send(msg.toString(), it) }
    }
}