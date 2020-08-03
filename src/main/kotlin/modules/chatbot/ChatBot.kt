package modules.chatbot

import modules.chatbot.chatBotEvents.LongPollEventBase
import java.util.concurrent.ConcurrentLinkedQueue


object ChatBot: Thread() {


    override fun run() {
        val queue = ConcurrentLinkedQueue<LongPollEventBase>()

        // longpolls
        //VkLongPoll(queue).start()
        DiscordLongPoll(queue).start()
        //TelegramLongPoll(queue).start()


        EventProcessor(queue).start()
    }

}