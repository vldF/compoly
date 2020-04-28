package modules.events.icsEvents

import log
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.Component
import net.fortuna.ical4j.model.component.VEvent

val calendarFiles = listOf("lms.ics") //TODO Сейчас есть только на lms, спасибо гуманитариям за календарь

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

            for (i in calendar.getComponents(Component.VEVENT)) {
                val calendarEvent = i as VEvent
                val summary = calendarEvent.summary
                val e = LocalEvent(
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