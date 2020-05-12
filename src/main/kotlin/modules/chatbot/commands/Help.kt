package modules.chatbot.commands

import api.Vk
import modules.Active
import modules.chatbot.ChatBot
import modules.chatbot.MessageNewObj
import java.lang.StringBuilder

@Active
class Help: Command {
    override val keyWord = listOf("/help")
    override val permission = CommandPermission.ALL
    override val description = "Список команд"

    override fun call(messageObj: MessageNewObj) {
        val result = StringBuilder()
        val commands = ChatBot.getCommands()
        for (permission in CommandPermission.values()) {
            result.append(commands.filter { it.permission == permission }
                .joinToString(
                    separator = "\n",
                    prefix = permission.helpHeaderString + ":\n",
                    postfix = "\n\n"
                ) { "${it.keyWord} - ${it.description}" })
        }
        Vk().send(result.toString(), listOf((messageObj.peer_id)))
    }
}