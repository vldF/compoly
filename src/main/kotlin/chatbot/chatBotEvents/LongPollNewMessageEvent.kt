package chatbot.chatBotEvents

import api.VkPlatform

/**
 * @constructor
 * time - время отправки сообщения в секундах, если время не указакно (такое может быть только в тестах),
 * то используется System.currentTimeMillis()
 */
open class LongPollNewMessageEvent(
        override val api: VkPlatform,
        open val chatId: Long,
        open val text: String,
        open val userId: Long,
        open val forwardMessageFromId: Long? = null,
        open val time: Long = System.currentTimeMillis() / 1000
) : LongPollEventBase()