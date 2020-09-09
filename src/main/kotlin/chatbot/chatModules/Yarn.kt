package chatbot.chatModules

import chatbot.ModuleObject
import chatbot.OnCommand
import chatbot.chatBotEvents.LongPollNewMessageEvent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@ExperimentalStdlibApi
@ModuleObject
object Yarn {
    var probability = 0
    @OnCommand(["нить", "yarn"], cost = 10, description = "Да найдите же ее кто-нибудь")
    fun loseYarn(event: LongPollNewMessageEvent) {
        event.api.send("Произвожу поиск...", event.chatId)
        probability++
        val delay = 3000L
        GlobalScope.launch {
            delay(delay)
            val found = Random.nextInt(0, 10_000)
            if (found <= probability)
                event.api.send("Товарищ, вы нашли нить! Этот день войдёт в историю.", event.chatId)
            else event.api.send("Нить потеряна", event.chatId)
        }
    }
}