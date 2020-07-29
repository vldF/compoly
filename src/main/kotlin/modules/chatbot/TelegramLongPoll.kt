package modules.chatbot

import api.TelegramPlatform
import api.TelegramUsersDataBase
import modules.chatbot.chatBotEvents.LongPollEventBase
import modules.chatbot.chatBotEvents.LongPollNewMessageEvent
import modules.chatbot.chatBotEvents.Platform
import telApiToken
import java.util.concurrent.ConcurrentLinkedQueue

class TelegramLongPoll(
        private val queue: ConcurrentLinkedQueue<LongPollEventBase>
): Thread() {
    private val telegram = TelegramPlatform
    override fun run() {
        var lastUpdateId = 0
        while(true) {
            val updates = telegram.getUpdates(lastUpdateId + 1) ?: continue
            for (update in updates) {
                val message = update.message ?: continue
                if (message.from.username != null) {
                    TelegramUsersDataBase.addId(message.from.id, message.from.username)
                }

                if (message.text == null) continue
                val messageEvent = LongPollNewMessageEvent(
                        Platform.TELEGRAM,
                        telegram,
                        message.chat.id,
                        message.text,
                        message.from.id
                )
                queue.add(messageEvent)
                lastUpdateId = update.update_id
            }
        }
    }
}


