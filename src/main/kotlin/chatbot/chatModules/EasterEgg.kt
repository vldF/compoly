package chatbot.chatModules

import chatbot.ModuleObject
import chatbot.OnCommand
import chatbot.chatBotEvents.LongPollNewMessageEvent

@ModuleObject
object EasterEgg {
    @OnCommand(["тождество"], cost = 40, showOnHelp = false)
    fun egg(event: LongPollNewMessageEvent) {
        event.api.send("== != !=\nЕсли ты, конечно, знаешь, что такое !=", event.chatId)
    }
}