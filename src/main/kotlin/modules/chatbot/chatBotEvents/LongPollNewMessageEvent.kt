package modules.chatbot.chatBotEvents

class LongPollNewMessageEvent(
        override val platform: Platform,
        override val chatId: Int,
        val text: String,
        val userId: Int
) : LongPollEventBase()