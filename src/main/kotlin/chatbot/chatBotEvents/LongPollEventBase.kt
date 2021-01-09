package chatbot.chatBotEvents

import api.VkPlatform

abstract class LongPollEventBase {
    abstract val platform: Platform
    abstract val api: VkPlatform
}

enum class Platform {
    VK,
    TELEGRAM,
    DISCORD
}
