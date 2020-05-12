package modules.chatbot.commands

import modules.chatbot.MessageNewObj

enum class CommandPermission(val helpHeaderString: String) {
    ALL("Общедоступные"),
    ADMIN_ONLY("Доступные только для администраторов")
}

interface Command {
    val keyWord: List<String>
    val permission: CommandPermission
    val description: String

    fun call(messageObj: MessageNewObj)
}