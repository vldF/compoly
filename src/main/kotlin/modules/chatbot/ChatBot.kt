package modules.chatbot

import io.github.classgraph.ClassGraph
import log
import modules.chatbot.chatBotEvents.LongPollEventBase
import java.util.concurrent.ConcurrentLinkedQueue


object ChatBot: Thread() {
    override fun run() {
        val queue = ConcurrentLinkedQueue<LongPollEventBase>()

        // longpolls
        VkLongPoll(queue).start()

        EventProcessor(queue).start()
    }

}