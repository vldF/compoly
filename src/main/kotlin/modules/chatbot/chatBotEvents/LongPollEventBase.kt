package modules.chatbot.chatBotEvents

abstract class LongPollEventBase(val platform: Platform)

enum class Platform {
    VK,
    TELEGRAM,
    DISCORD
}
