package chatbot.listeners

import chatbot.CommandPermission
import java.lang.reflect.Method

data class CommandListener(
    val commands: Array<String>,
    val description: String,
    val classInstance: Any,
    val method: Method,
    val permission: CommandPermission,
    val showInHelp: Boolean,
    val controlUsage: Boolean,
    val levelBonus: Int,
    val baseUsageAmount: Int,
    val notEnoughMessage: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        other as CommandListener

        if (!commands.contentEquals(other.commands)) return false
        if (description != other.description) return false
        if (method != other.method) return false
        if (permission != other.permission) return false
        if (showInHelp != other.showInHelp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = commands.contentHashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + classInstance.hashCode()
        result = 31 * result + method.hashCode()
        result = 31 * result + permission.hashCode()
        result = 31 * result + showInHelp.hashCode()
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