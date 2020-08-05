package modules.chatbot

import api.TelegramPlatform
import api.TelegramUsersDataBase
import modules.chatbot.chatBotEvents.LongPollEventBase
import modules.chatbot.chatBotEvents.LongPollNewTGMessageEvent
import modules.chatbot.chatBotEvents.Platform
import java.util.concurrent.ConcurrentLinkedQueue

class TelegramLongPoll(
        private val queue: ConcurrentLinkedQueue<LongPollEventBase>
): Thread() {
    private val telegram = TelegramPlatform
    override fun run() {
        var lastUpdateId = 0
        while(true) {
            val updates = telegram.getUpdates(lastUpdateId + 1) ?: continue
            if(updates.size > 10) {
                lastUpdateId = updates.last().update_id
                continue
            }
            for (update in updates) {
                val message = update.message ?: continue
                if (message.from.username != null) {
                    TelegramUsersDataBase.addId(message.from.id, message.from.username)
                }
                lastUpdateId = update.update_id
                if (message.text == null && message.dice == null) continue

                val messageEvent = LongPollNewTGMessageEvent(
                        Platform.TELEGRAM,
                        telegram,
                        message.chat.id,
                        message.text ?: "/${message.dice?.emoji}" ?: "",
                        message.from.id,
                        message.dice?.value,
                        message.reply_to_message?.from?.id
                )
                queue.add(messageEvent)

            }
        }
    }
}


