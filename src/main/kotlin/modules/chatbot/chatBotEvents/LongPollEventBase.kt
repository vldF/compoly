package modules.chatbot.chatBotEvents

import api.PlatformApiInterface

abstract class LongPollEventBase {
    abstract val platform: Platform
    abstract val chatId: Long
    abstract val api: PlatformApiInterface
}

enum class Platform {
    VK,
    TELEGRAM,
    DISCORD
}
