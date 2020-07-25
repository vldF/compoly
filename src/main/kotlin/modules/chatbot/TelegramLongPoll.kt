package modules.chatbot

import api.TelegramPlatform
import modules.chatbot.chatBotEvents.LongPollEventBase
import modules.chatbot.chatBotEvents.LongPollNewMessageEvent
import modules.chatbot.chatBotEvents.Platform
import java.util.concurrent.ConcurrentLinkedQueue

class TelegramLongPoll(
        private val queue: ConcurrentLinkedQueue<LongPollEventBase>
): Thread() {
    private val telegram = TelegramPlatform()
    override fun run() {
        var lastUpdateId = 0
        while(true) {
            val updates = telegram.getUpdates(lastUpdateId + 1) ?: continue
            for (update in updates) {
                if (update.message?.text == null) continue
                val messageEvent = LongPollNewMessageEvent(
                        Platform.VK,
                        telegram,
                        update.message.chat.id,
                        update.message.text,
                        update.message.from.id
                )
                queue.add(messageEvent)
                lastUpdateId = update.update_id
            }
        }
    }
}


