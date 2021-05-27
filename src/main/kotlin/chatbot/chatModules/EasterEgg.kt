package chatbot.chatModules

import api.GarbageMessage.Companion.toGarbageMessageWithDelay
import api.GarbageMessagesCollector
import api.GarbageMessagesCollector.Companion.DEFAULT_DELAY
import chatbot.ModuleObject
import chatbot.OnCommand
import chatbot.chatBotEvents.LongPollNewMessageEvent

@ModuleObject
object EasterEgg {
    @OnCommand(["тождество"], cost = 40, showOnHelp = false)
    fun egg(event: LongPollNewMessageEvent) {
        event.api.send("== != !=\nЕсли ты, конечно, знаешь, что такое !=", event.chatId, removeDelay = DEFAULT_DELAY)
        GarbageMessagesCollector.addGarbageMessage(event.toGarbageMessageWithDelay(0))
    }
}