package modules.events.icsEvents

import log
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.Component
import net.fortuna.ical4j.model.Property
import net.fortuna.ical4j.model.component.CalendarComponent
import net.fortuna.ical4j.model.component.VEvent

val calendarFiles = listOf("lms.ics")

/**
 * Reads local events from the calendars
 */
class Reader {
    fun read(): List<LocalEvent> {
        val res = mutableListOf<LocalEvent>()
        val calendarBuilder = CalendarBuilder()
        for (fileName in calendarFiles) {
            val file = javaClass.getResourceAsStream("/$fileName")
            val calendar = calendarBuilder.build(file.reader())

            for (i in calendar.getComponents<CalendarComponent>(Component.VEVENT)) {
                val calendarEvent = i as VEvent
                val summary = calendarEvent.summary
                val e = LocalEvent(
                    summary.value,
                    calendarEvent.startDate.date.time,
                    calendarEvent.endDate.date.time,
                    calendarEvent.getProperty<Property>("CATEGORIES").value
                )
                res.add(e)
            }
        }
        log.info("Read: ${res.size} events")
        return res
    }
}