package modules.events.daysUntil

import api.VkPlatform
import chatIds
import getTime
import modules.events.Event
import modules.events.Time
import modules.events.daysUntil
import java.text.SimpleDateFormat
import java.util.*


class DaysUntil : Event {
    override val schedule = listOf(Time(8, 30))
    override val name = "Days until..."

    private val myFormat = SimpleDateFormat("dd MM yyyy HH")
    private val vk = VkPlatform
    private val days = listOf(
            Day("☀Дней до начала лета: ", myFormat.parse("01 06 2020 23"), "☀☀☀☀☀☀☀☀"),
            Day("\uD83D\uDCD9Дней до физики: ", myFormat.parse("08 06 2020 23"), "Время сдавать физику...")
    )

    override fun call() {
        for (day in days) {
            val daysUntil = daysUntil(Date(getTime()), day.date)
            if (daysUntil > 0) {
                chatIds.forEach { vk.send(day.message + daysUntil, it) }
            } else if (daysUntil == 0L) {
                chatIds.forEach { vk.send(day.finalMessage, it) }
            }
        }
    }

    class Day(val message: String, val date: Date, val finalMessage: String)
}