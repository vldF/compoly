package chatbot.chatBotEvents

import api.PlatformApiInterface

open class LongPollNewMessageEvent(
        override val platform: Platform,
        override val api: PlatformApiInterface,
        open val chatId: Long,
        open val text: String,
        open val userId: Long,
        open val forwardMessageFromId: Long? = null
) : LongPollEventBase()