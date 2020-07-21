package modules.chatbot

import modules.chatbot.chatBotEvents.LongPollEventBase
import java.util.concurrent.ConcurrentLinkedQueue

class TelegramLongPoll(
        private val queue: ConcurrentLinkedQueue<LongPollEventBase>
): Thread() {
    override fun run() {
        var lastUpdateId = 0
        while(true) {
            val updates = bot.getUpdates(lastUpdateId + 1) ?: continue
            for (update in updates) {
                queue.add(update)
                lastUpdateId = update.update_id
            }
        }
    }
}