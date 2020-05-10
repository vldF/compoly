package modules.chatbot.commands

import api.Vk
import modules.Active
import modules.chatbot.MessageNewObj

@Active
class Cats: Command {
    override val keyWord = "/cat"
    override val permission = CommandPermission.ADMIN_ONLY

    val theCatApiKey = "dc64b39c-51b6-43aa-ba44-a231e8937d5b"

    override fun call(messageObj: MessageNewObj) {


    }
}

