package modules.chatbot.chatModules

import api.Vk
import modules.Active
import modules.chatbot.ChatBot
import modules.chatbot.CommandPermission
import modules.chatbot.MessageNewObj
import modules.chatbot.OnCommand
import java.lang.StringBuilder

@Active
class Help {
    @OnCommand(["help", "помощь"], "Отображение справки (из дурки)")
    fun help(messageObj: MessageNewObj) {
        val result = StringBuilder()
        val commands = ChatBot.getCommands()
        for (permission in CommandPermission.values()) {
            result.append(commands.filter { it.permission == permission }
                .joinToString(
                    separator = "\n",
                    prefix = permission.helpHeaderString + ":\n",
                    postfix = "\n\n"
                ) { "${it.commands.first()} - ${it.description}" })
        }
        Vk().send(result.toString(), listOf((messageObj.peer_id)))
    }
}