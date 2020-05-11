package modules.chatbot.commands

import modules.chatbot.MessageNewObj

enum class CommandPermission {
    ALL,
    ADMIN_ONLY
}

interface Command {
    val keyWord: List<String>
    val permission: CommandPermission

    fun call(messageObj: MessageNewObj)
}