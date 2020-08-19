package modules.chatbot.chatModules

import modules.chatbot.CommandPermission
import modules.chatbot.ModuleObject
import modules.chatbot.OnCommand
import modules.chatbot.chatBotEvents.LongPollNewMessageEvent

@ModuleObject
object Ping {
    @OnCommand(["пинг", "ping"], "Pong!", CommandPermission.ADMIN, showOnHelp = false)
    fun ping(event: LongPollNewMessageEvent) {
        event.api.send("Pong!", event.chatId)
    }
}