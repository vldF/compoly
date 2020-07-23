package api
import log
import java.util.concurrent.ConcurrentLinkedQueue

object SendMessageThread: Thread() {
    private var messages: ConcurrentLinkedQueue<Message> = ConcurrentLinkedQueue()
    private const val maxMessagesInOneSession = 7
    private val vk = VkPlatform()

    override fun run() {
        while (true) {
            if (messages.isNotEmpty()) {
                var count = 0
                while (messages.isNotEmpty()) {
                    val message = messages.poll()

                    val text = message.message
                    val attachments = message.attachments.joinToString(",")
                    val chatIds = message.chatIds.map {
                        if (it >= 100000000) it - 2000000000 else it
                    }
                    log.info("text: $text")
                    for (id in chatIds) {
                        log.info(text)
                        count++
                        vk.post("messages.send", mutableMapOf(
                            "message" to text,
                            "chat_id" to id,
                            "random_id" to System.currentTimeMillis().toString(),
                            "attachment" to attachments))
                        if (count == maxMessagesInOneSession) {
                            sleep(3000)
                            count = 0
                        }
                    }
                }
            } else
                sleep(20)
        }
    }

    fun addInList(message: Message) {
        if (messages.size > 50)
            return
        messages.add(message)
    }
}

data class Message(
        val message: String = "",
        val chatIds: List<Long>,
        val attachments: List<String>
)