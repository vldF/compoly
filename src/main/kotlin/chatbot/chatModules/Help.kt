package chatbot.chatModules

import chatbot.*
import chatbot.chatBotEvents.LongPollNewMessageEvent

@ModuleObject
object Help {
    @OnCommand(["помощь", "help", "h", "?"], "отображение справки (из дурки)")
    fun help(event: LongPollNewMessageEvent) {
        val api = event.api
        val result = StringBuilder()
        val usersPermissions = Permissions.getUserPermissionsByNewMessageEvent(event)
        val commands = EventProcessor.commandListeners.filter { it.showInHelp }

        val permissions = CommandPermission.values().filter {
            it.ordinal <= usersPermissions.ordinal
        }

        for (permission in permissions) {
            result.append(commands.filter { it.permission == permission && it.description.isNotEmpty()}
                .joinToString(
                    separator = "\n",
                    prefix = permission.helpHeaderString + ":\n",
                    postfix = "\n\n"
                ) { "/${it.commands.first()} — ${it.description}" })
        }
        api.send(result.toString(), event.chatId)
    }
}
