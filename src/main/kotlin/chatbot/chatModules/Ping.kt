package chatbot.chatModules

import api.GarbageMessage.Companion.toGarbageMessageWithDelay
import api.GarbageMessagesCollector
import api.GarbageMessagesCollector.Companion.DEFAULT_DELAY
import chatbot.ModuleObject
import chatbot.OnCommand
import chatbot.chatBotEvents.LongPollNewMessageEvent

@ModuleObject
object Ping {
    @OnCommand(["пинг", "ping"], "Pong!")
    fun ping(event: LongPollNewMessageEvent) {
        event.api.send("Pong!", event.chatId, removeDelay = DEFAULT_DELAY)
        GarbageMessagesCollector.addGarbageMessage(event.toGarbageMessageWithDelay(0))
    }
}