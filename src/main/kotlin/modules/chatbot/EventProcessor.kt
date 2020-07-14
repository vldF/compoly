package modules.chatbot

import log
import modules.chatbot.chatBotEvents.LongPollEventBase
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors

class EventProcessor(private val queue: ConcurrentLinkedQueue<LongPollEventBase>) : Thread() {
    private val pollSize = 4
    private val poll = Executors.newFixedThreadPool(pollSize)

    private fun mainLoop() {
        while (true) {
            val event = queue.poll()

            poll.submit {
                process(event)
            }
        }
    }

    private fun process(event: LongPollEventBase) {
        TODO()
    }

    override fun run() {
        log.info("Starting event processor")
        mainLoop()
    }
}