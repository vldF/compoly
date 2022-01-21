package api

import chatbot.chatBotEvents.LongPollNewMessageEvent
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicLong

class GarbageMessagesCollector : Thread() {
    companion object {
        const val DEFAULT_DELAY: Long = 1000 * 10
        const val MINUTE_DELAY: Long = 1000 * 60

        private val queue: LinkedBlockingQueue<GarbageMessage> = LinkedBlockingQueue()

        fun deleteMessageOnTime(messageId: Int, chatId: Int, timeWhenDelete: Long) {
            queue.add(GarbageMessage(messageId, chatId, timeWhenDelete))
        }

        /**
         * @param delay: delay before message will be deleted, ms
         */
        fun deleteMessageWithDelay(messageId: Int, chatId: Int, delay: Long) {
            val deleteTime = System.currentTimeMillis() + delay
            deleteMessageOnTime(messageId, chatId, deleteTime)
        }

        /** @param time: time that can be increased or decreased, when message will be deleted, ms */
        fun deleteMessageOnDynamicTime(messageId: Int, chatId: Int, time: AtomicLong) {
            queue.add(GarbageMessage(messageId, chatId, dynamicDeleteTime = time))
        }

        fun addGarbageMessage(message: GarbageMessage) {
            queue.add(message)
        }
    }

    override fun run() {
        try {
            while (true) {
                val element = queue.take()

                if (element.messageId == 0) {
                    continue // remove after VK will send message id
                }

                val currentTime = System.currentTimeMillis()

                val notTimeYet = if (element.dynamicDeleteTime == null) {
                    element.deleteTime - currentTime > 0
                } else {
                    element.dynamicDeleteTime.get() - currentTime > 0
                }

                if (notTimeYet) {
                    queue.add(element) // return element to queue
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
    val deleteTime: Long = 0,
    val dynamicDeleteTime: AtomicLong? = null
) {
    companion object {
        fun LongPollNewMessageEvent.toGarbageMessageWithDeleteTime(deleteTime: Long): GarbageMessage {
            return GarbageMessage(
                messageId,
                chatId,
                deleteTime
            )
        }

        /**
         * @param delay: delay before message wil be deleted, ms
         */
        fun LongPollNewMessageEvent.toGarbageMessageWithDelay(delay: Long): GarbageMessage {
            return GarbageMessage(
                messageId,
                chatId,
                System.currentTimeMillis() + delay
            )
        }
    }
}