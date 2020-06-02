package modules.chatbot.listeners

import modules.chatbot.CommandPermission
import java.lang.reflect.Method

data class CommandListener(
        val commands: Array<String>,
        val description: String,
        val baseClass: Any,
        val call: Method,
        val permission: CommandPermission,
        val cost: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CommandListener) return false

        if (!commands.contentEquals(other.commands)) return false
        if (baseClass != other.baseClass) return false
        if (call != other.call) return false
        if (permission != other.permission) return false

        return true
    }

    override fun hashCode(): Int {
        var result = commands.contentHashCode()
        result = 31 * result + baseClass.hashCode()
        result = 31 * result + call.hashCode()
        result = 31 * result + permission.hashCode()
        return result
    }
}