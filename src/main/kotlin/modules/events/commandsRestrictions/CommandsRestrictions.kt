package modules.events.commandsRestrictions

import chatbot.chatModules.RatingSystem
import modules.Active
import modules.events.Event
import modules.events.Time

@Active
class CommandsRestrictions : Event {
    override val name = "Обновление ограничений по командам"
    override val schedule: List<Time> = listOf(
        Time(0, 0),
        Time(4, 0),
        Time(8, 0),
        Time(12, 0),
        Time(16, 0),
        Time(20, 0)
    )
    override fun call() {
        RatingSystem.updateCommandsRestrictions()
    }
}