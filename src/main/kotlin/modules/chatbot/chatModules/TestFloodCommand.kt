package modules.chatbot.chatModules

import modules.chatbot.ModuleObject
import modules.chatbot.OnCommand
import modules.chatbot.chatBotEvents.LongPollNewMessageEvent

@ModuleObject
object TestFloodCommand {

    @OnCommand(["flood"], "")
    fun foo(event: LongPollNewMessageEvent) {
        for (i in 0..30) {
            event.api.send("test flood message #$i", event.chatId)
        }
    }
}