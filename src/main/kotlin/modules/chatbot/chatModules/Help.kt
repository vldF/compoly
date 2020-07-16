package modules.chatbot.chatModules

import api.VkPlatform
import modules.Active
import modules.chatbot.ChatBot
import modules.chatbot.CommandPermission
import modules.chatbot.MessageNewObj
import modules.chatbot.OnCommand
import java.lang.StringBuilder
/*
@Active
class Help {
    private val vk = VkPlatform()

    @OnCommand(["помощь", "help", "h", "?"], "отображение справки (из дурки)")
    fun help(messageObj: MessageNewObj) {
        val result = StringBuilder()
        val usersPermissions = ChatBot.getPermission(messageObj)
        val commands = ChatBot.getCommands()

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
        vk.send(result.toString(), messageObj.peer_id)
    }
}
*/