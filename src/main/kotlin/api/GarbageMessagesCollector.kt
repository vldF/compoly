package api

import chatbot.chatBotEvents.LongPollNewMessageEvent
import java.util.concurrent.LinkedBlockingQueue

class GarbageMessagesCollector : Thread() {
    companion object {
        private val queue: LinkedBlockingQueue<GarbageMessage> = LinkedBlockingQueue()

        fun deleteMessageOnTime(messageId: Int, chatId: Int, timeWhenDelete: Long) {
            queue.add(GarbageMessage(messageId, chatId, timeWhenDelete))
        }

        /**
         * @param delay: delay before message wil be deleted, ms
         */
        fun deleteMessageWithDelay(messageId: Int, chatId: Int, delay: Long) {
            val deleteTime = System.currentTimeMillis() + delay
            deleteMessageOnTime(messageId, chatId, deleteTime)
        }

        fun addGarbageMessage(message: GarbageMessage) {
            queue.add(message)
        }
    }

    override fun run() {
        try {
            while (true) {
                val element = queue.take()

                if (element.deleteTime - System.currentTimeMillis() >= 0) {
                    queue.add(element)
                    sleep(30)
                    continue
                }

                VkApi.deleteMessage(element.messageId, element.chatId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            this.run()
        }
    }
}

data class GarbageMessage(
    val messageId: Int,
    val chatId: Int,
    val deleteTime: Long
) {
    companion object {
        fun toGarbageMessageWithDeleteTime(event: LongPollNewMessageEvent, deleteTime: Long): GarbageMessage {
            return GarbageMessage(
                event.messageId,
                event.chatId,
                deleteTime
            )
        }

        /**
         * @param delay: delay before message wil be deleted, ms
         */
        fun toGarbageMessageWithDelay(event: LongPollNewMessageEvent, delay: Long): GarbageMessage {
            return GarbageMessage(
                event.messageId,
                event.chatId,
                System.currentTimeMillis() + delay
            )
        }
    }
}