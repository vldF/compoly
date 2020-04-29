package modules.events.summer

import api.Vk
import chatIds
import modules.Active
import modules.events.Event
import modules.events.Time
import modules.events.daysUntil
import java.text.SimpleDateFormat
import java.util.*

@Active
class Summer : Event {

    override val schedule = listOf(Time(8, 1))
    override val name = "Days until summer"

    private val myFormat = SimpleDateFormat("dd MM yyyy")
    private val summerBegins = myFormat.parse("01 06 2020")

    override fun call() {
        val days = daysUntil(Date(), summerBegins)
        if (days > 0) {
            Vk().send("☀Дней до начала лета: $days", chatIds)
        } else if (days == 0L) Vk().send("☀".repeat(10000), chatIds)
    }
}