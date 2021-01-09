package chatbot

import chatbot.chatBotEvents.LongPollEventBase
import java.util.concurrent.ConcurrentLinkedQueue


object ChatBot: Thread() {
    override fun run() {
        val queue = ConcurrentLinkedQueue<LongPollEventBase>()
        VkLongPoll(queue).start()
        EventProcessor(queue).start()
    }

}