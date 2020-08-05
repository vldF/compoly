package modules.chatbot.chatBotEvents

import api.PlatformApiInterface

open class LongPollNewMessageEvent(
        override val platform: Platform,
        override val api: PlatformApiInterface,
        override val chatId: Long,
        val text: String,
        val userId: Long,
        val forwardMessageFromId: Long? = null
) : LongPollEventBase()