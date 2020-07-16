package modules.chatbot.chatBotEvents

abstract class LongPollEventBase {
    abstract val platform: Platform
    abstract val chatId: Int
}

enum class Platform {
    VK,
    TELEGRAM,
    DISCORD
}
