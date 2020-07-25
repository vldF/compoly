package modules.chatbot.chatModules

import modules.Active
import modules.chatbot.CommandPermission
import modules.chatbot.EventProcessor
import modules.chatbot.OnCommand
import modules.chatbot.Permissions
import modules.chatbot.chatBotEvents.LongPollNewMessageEvent

@Active
class Help {
    @OnCommand(["помощь", "help", "h", "?"], "отображение справки (из дурки)")
    fun help(event: LongPollNewMessageEvent) {
        val api = event.api
        val result = StringBuilder()
        val usersPermissions = Permissions.getUserPermissionsByNewMessageEvent(event)
        val commands = EventProcessor.commandListeners

        val permissions = CommandPermission.values().filter {
            it.ordinal <= usersPermissions.ordinal
        }

        for (permission in permissions) {
            result.append(commands.filter { it.permission == permission && it.description.isNotEmpty()}
                .joinToString(
                    separator = "\n",
                    prefix = permission.helpHeaderString + ":\n",
                    postfix = "\n\n"
                ) { "/${it.commands.first()} [${it.cost}] — ${it.description}" })
        }
        api.send(result.toString(), event.chatId)
    }
}
