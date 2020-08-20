package chatbot.chatModules

import chatbot.ModuleObject
import chatbot.OnCommand
import chatbot.chatBotEvents.LongPollNewMessageEvent

@ModuleObject
object TestFloodCommand {
    @OnCommand(["flood"], "")
    fun foo(event: LongPollNewMessageEvent) {
        for (i in 0..30) {
            event.api.send("test flood message #$i", event.chatId)
        }
    }
}