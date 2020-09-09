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
    @OnCommand(["нить", "yarn"], cost = 10, description = "Да найдите же ее кто-нибудь")
    fun loseYarn(event: LongPollNewMessageEvent) {
        event.api.send("произвожу поиск...", event.chatId)
        val delay = 3000L
        GlobalScope.launch {
            delay(delay)
            event.api.send("нить потеряна", event.chatId)
        }
    }
}