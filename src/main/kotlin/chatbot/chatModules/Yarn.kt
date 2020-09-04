package chatbot.chatModules

import chatbot.ModuleObject
import chatbot.OnCommand
import chatbot.chatBotEvents.LongPollNewMessageEvent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@ExperimentalStdlibApi
@ModuleObject
object Yarn {
    @OnCommand(["yarn", "нить"], "Да найдите же ее кто-нибудь", cost = 10)
    fun loseYarn(event: LongPollNewMessageEvent) {
        event.api.send("произвожу поиск...", event.chatId)
        val delay = 3000L
        GlobalScope.launch {
            delay(delay)
            event.api.send("нить потеряна", event.chatId)
        }
    }
}