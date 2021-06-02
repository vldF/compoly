package chatbot.listeners

import chatbot.CommandPermission
import java.lang.reflect.Method

data class CommandListener(
        val commands: Array<String>,
        val description: String,
        val baseClass: Any,
        val call: Method,
        val permission: CommandPermission,
        val showOnHelp: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        other as CommandListener

        if (!commands.contentEquals(other.commands)) return false
        if (description != other.description) return false
        if (call != other.call) return false
        if (permission != other.permission) return false
        if (showOnHelp != other.showOnHelp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = commands.contentHashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + baseClass.hashCode()
        result = 31 * result + call.hashCode()
        result = 31 * result + permission.hashCode()
        result = 31 * result + showOnHelp.hashCode()
        return result
    }
}

data class VirtualCommandBody(
    val commandId: Int,
    val chatId: Int,
    val triggers: List<String>,
    var text: String,
    var attachments: String
)