package chatbot.chatBotEvents

import api.VkPlatform

open class LongPollNewMessageEvent(
        override val platform: Platform,
        override val api: VkPlatform,
        open val chatId: Long,
        open val text: String,
        open val userId: Long,
        open val forwardMessageFromId: Long? = null
) : LongPollEventBase()