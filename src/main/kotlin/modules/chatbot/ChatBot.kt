package modules.chatbot

import io.github.classgraph.ClassGraph
import log
import modules.chatbot.chatBotEvents.LongPollEventBase
import modules.chatbot.listeners.CommandListener
import modules.chatbot.listeners.MessageListener
import java.util.concurrent.ConcurrentLinkedQueue


object ChatBot: Thread() {
    override fun run() {
        log.info("Initializing ChatBot")
        log.info("Initializing modules done")

        val queue = ConcurrentLinkedQueue<LongPollEventBase>()

        // longpolls
        VkLongPoll(queue).start()

        EventProcessor(queue).start()
    }

}