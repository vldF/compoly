package chatbot.chatBotEvents

import api.VkPlatform

open class LongPollNewMessageEvent(
        override val api: VkPlatform,
        open val chatId: Long,
        open val text: String,
        open val userId: Long,
        open val forwardMessageFromId: Long? = null,
        open val unixTime: Long = System.currentTimeMillis() / 1000
) : LongPollEventBase()