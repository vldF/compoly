package modules.chatbot.chatModules

import modules.chatbot.CommandPermission
import modules.chatbot.ModuleObject
import modules.chatbot.OnCommand
import modules.chatbot.chatBotEvents.LongPollNewMessageEvent

@ModuleObject
object Ping {
    @OnCommand(["ping", "пинг"], "Pong!", CommandPermission.ADMIN)
    fun ping(event: LongPollNewMessageEvent) {
        event.api.send("Pong!", event.chatId)
    }
}