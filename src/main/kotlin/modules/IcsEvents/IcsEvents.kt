package modules.IcsEvents

import api.Vk
import vkApiToken
import java.text.SimpleDateFormat
import kotlin.math.abs

class IcsEvents : modules.Module {
    override val name = "Проверка ивентов в расписаниях"
    private val formatter = SimpleDateFormat("EEEE, d MMMM yyyy")

    override fun call() {
        val reader = Reader()
        val currentTime = System.currentTimeMillis()

        val currentEvents = mutableListOf<String>()
        val nextEvents = mutableListOf<String>()

        for (evn in reader.read()) {
            val s = "[${evn.category}] ${evn.name}:\n${formatter.format(evn.dateStart)}\n"
            // todo: refactor next code
            (if (abs(currentTime / (1000 * 60 * 60 * 24) - evn.dateStart / (1000 * 60 * 60 * 24)) <= 1) currentEvents else if (evn.dateStart - currentTime in 0..5 * 24 * 60 * 60 * 1000) nextEvents else mutableListOf()).add(
                s
            )
        }

        val msg = StringBuilder("\uD83D\uDDD3Сегодня:\n")
        if (currentEvents.isEmpty()) msg.append("Ничего\n")
        msg.append(currentEvents.joinToString(separator = "\n"))
        msg.append("\n\uD83D\uDDD3Ближайшие ивенты (3 дня):\n")
        if (nextEvents.isEmpty()) msg.append("Пусто\n")
        msg.append(nextEvents.joinToString(separator = "\n"))
        Vk(vkApiToken).send(msg.toString(), "1")
    }
}