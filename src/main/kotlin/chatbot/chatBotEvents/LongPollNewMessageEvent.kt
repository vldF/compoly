package chatbot.chatBotEvents

import chatbot.Attachment
import api.VkApi

/**
 * @constructor
 * time - время отправки сообщения в секундах, если время не указанно (такое может быть только в тестах),
 * то используется System.currentTimeMillis()
 */
open class LongPollNewMessageEvent(
    override val api: VkApi,
    open val chatId: Int,
    open val text: String,
    open val userId: Int,
    open val forwardMessageFromId: Int? = null,
    open val time: Long = System.currentTimeMillis() / 1000,
    open val attachments: List<Attachment>? = null,
    open val messageId: Int
) : LongPollEventBase()