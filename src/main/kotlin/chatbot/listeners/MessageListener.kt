package chatbot.listeners

import java.lang.reflect.Method

data class MessageListener(
        val baseClass: Any,
        val call: Method
)