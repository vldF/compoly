package modules.chatbot.commands

import api.Vk
import modules.Active
import modules.chatbot.MessageNewObj

@Active
class Cats: Command {
    override val keyWord = "/cat"
    override val permission = CommandPermission.ADMIN_ONLY

    override fun call(messageObj: MessageNewObj) {

        var answer = ""
        Vk().send(answer, listOf(messageObj.peer_id.toString()))
    }
}

