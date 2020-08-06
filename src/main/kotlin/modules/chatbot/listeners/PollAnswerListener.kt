package modules.chatbot.listeners

import java.lang.reflect.Method

data class PollAnswerListener (
        val baseClass: Any,
        val call: Method
)