package modules.events.schedule

import api.VkApi
import chatbot.chatModules.Schedule
import chatbot.chatModules.Schedule.dayNow
import modules.Active
import modules.events.Event
import modules.events.Time

@Active
class ScheduleEvent : Event {
    override val name = "Расписание"
    override val schedule: List<Time> = listOf(Time(8, 0))

    override fun call() {
        Schedule.onDay(VkApi, dayNow())
    }
}