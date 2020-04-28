package modules.events.summer

import api.Vk
import chatIds
import modules.events.Event
import modules.events.Time
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class Summer : Event {

    override val schedule = listOf(Time(8, 1))
    override val name = "Дней до лета"

    private val myFormat = SimpleDateFormat("dd MM yyyy")
    private val summerBegins = myFormat.parse("01 06 2020")

    override fun call() {
        val days = getDifferenceDays(Date(), summerBegins)
        if (days > 0) {
            Vk().send("☀Дней до начала лета: $days", chatIds)
        }
    }

    private fun getDifferenceDays(d1: Date, d2: Date): Long {
        val diff: Long = d2.time - d1.time
        return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)
    }
}