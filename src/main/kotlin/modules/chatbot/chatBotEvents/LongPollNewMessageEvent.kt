package modules.chatbot.chatBotEvents

class LongPollNewMessageEvent(
        platform: Platform,
        val text: String,
        val senderId: Int,
        val chatId: Int
) : LongPollEventBase(platform)