package chatbot.chatModules

import chatbot.ModuleObject
import chatbot.OnCommand
import chatbot.UsageInfo
import chatbot.chatBotEvents.LongPollNewMessageEvent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@ExperimentalStdlibApi
@ModuleObject
object Yarn {
    private const val notEnoughMessage = "Товарищ, ваши запросы на поиск нити закончились. Обновление запаса нитей происходит раз в 12 часов"
    private var probability = 0

    @UsageInfo(baseUsageAmount = 6, levelBonus = 2, notEnoughMessage)
    @OnCommand(["нить", "yarn"], description = "Да найдите же ее кто-нибудь")
    fun yarn(event: LongPollNewMessageEvent) {
        loseYarn(event)
    }

    private fun loseYarn(event: LongPollNewMessageEvent) {
        probability++
        val delay = 3000L
        event.api.send("Произвожу поиск...", event.chatId, removeDelay = delay)
        GlobalScope.launch {
            delay(delay)
            val found = Random.nextInt(0, 500)
            if (found <= probability) {
                event.api.send("Товарищ, вы нашли нить! Этот день войдёт в историю.", event.chatId)
                probability = 0
            } else {
                event.api.send("Нить потеряна", event.chatId)
            }
        }
    }
}