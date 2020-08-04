package modules.chatbot.chatBotEvents

import api.PlatformApiInterface
import api.TelegramPlatform

class LongPollNewTGMessageEvent(
        platform: Platform,
        api: TelegramPlatform,
        chatId: Long,
        text: String,
        userId: Long,
        val diceResult: Int?
): LongPollNewMessageEvent(platform, api, chatId, text, userId)
