package chatbot.chatModules

import chatbot.CommandPermission
import chatbot.ModuleObject
import chatbot.OnCommand
import chatbot.chatBotEvents.LongPollNewMessageEvent

@ModuleObject
object Ping {
    @OnCommand(["пинг", "ping"], "Pong!", CommandPermission.ADMIN, showOnHelp = false)
    fun ping(event: LongPollNewMessageEvent) {
        event.api.send("Pong!", event.chatId)
    }
}