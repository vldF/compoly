package modules.events.daysUntil

import api.Vk
import chatIds
import getTime
import modules.Active
import modules.events.Event
import modules.events.Time
import modules.events.daysUntil
import java.text.SimpleDateFormat
import java.util.*

@Active
class DaysUntil : Event {

    override val schedule = listOf(Time(8, 30))
    override val name = "Days until..."

    private val myFormat = SimpleDateFormat("dd MM yyyy")

    class Day(val message: String, val date: Date, val finalMessage: String)
    private val days = listOf(
            Day("☀Дней до начала лета: ", myFormat.parse("01 06 2020"), "☀☀☀☀☀☀☀☀"),
            Day("\uD83D\uDCD9Дней до физики: ", myFormat.parse("08 06 2020"), "Время сдавать физику...")
    )

    override fun call() {
        for (day in days) {
            val daysUntil = daysUntil(Date(getTime()), day.date)
            if (daysUntil > 0) {
                Vk().send(day.message + daysUntil, chatIds)
            } else if (daysUntil == 0L) {
                Vk().send(day.finalMessage, chatIds)
            }
        }
    }
}