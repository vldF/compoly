package modules.icsEvents

import log
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.Component
import net.fortuna.ical4j.model.component.VEvent

val calendarFiles = listOf("English.ics", "Main.ics")
class Reader {
    fun read(): List<Event> {
        val res = mutableListOf<Event>()
        val calendarBuilder = CalendarBuilder()
        for (fileName in calendarFiles) {
            val file = javaClass.getResourceAsStream("/$fileName")
            val calendar = calendarBuilder.build(file.reader())

            for (i in calendar.getComponents(Component.VEVENT)) {
                val calendarEvent = i as VEvent
                val summary = calendarEvent.summary
                val e = Event(
                    summary.value,
                    calendarEvent.startDate.date.time,
                    calendarEvent.endDate.date.time,
                    calendarEvent.getProperty("CATEGORIES").value
                )
                res.add(e)
            }
        }
        log.info("read: ${res.size} events")
        return res
    }
}