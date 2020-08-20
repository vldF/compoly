package chatbot.chatBotEvents

import api.PlatformApiInterface

class LongPollDSNewMessageEvent(
        override val platform: Platform,
        override val api: PlatformApiInterface,
        override val chatId: Long,
        override val text: String,
        override val userId: Long,
        val guildId: Long
) : LongPollNewMessageEvent(platform, api, chatId, text, userId)