package api

import chatbot.chatBotEvents.LongPollNewMessageEvent
import chatbot.chatModules.misc.Voting
import java.util.concurrent.LinkedBlockingQueue

class GarbageMessagesCollector : Thread() {
    companion object {
        const val DEFAULT_DELAY: Long = 10000
        const val MINUTE_DELAY: Long = 1000 * 60

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

        fun deleteMessageOnTimeIsUp(messageId: Int, chatId: Int, voting: Voting) {
            queue.add(GarbageMessage(messageId, chatId, voting = voting))
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

                val timeIsUp = if (element.voting == null) {
                    element.deleteTime - System.currentTimeMillis() > 0
                } else {
                    element.voting.timeOfClosing * 1000 - System.currentTimeMillis() > 0
                }

                if (timeIsUp) {
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
    val voting: Voting? = null
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