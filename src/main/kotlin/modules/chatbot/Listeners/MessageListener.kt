package modules.chatbot.Listeners

import modules.chatbot.CommandPermission
import java.lang.reflect.Method

data class MessageListener(
        val baseClass: Any,
        val call: Method
)