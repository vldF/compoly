package chatbot.chatBotEvents

import api.TelegramPlatform

class LongPollNewTGMessageEvent(
        platform: Platform,
        api: TelegramPlatform,
        chatId: Long,
        text: String,
        userId: Long,
        val diceResult: Int?,
        messageForwardedFromId: Long?,
        val pollId: String?
): LongPollNewMessageEvent(platform, api, chatId, text, userId, messageForwardedFromId)
