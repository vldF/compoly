package chatbot.listeners

import java.lang.reflect.Method

data class PollListener(
        val baseClass: Any,
        val call: Method
)