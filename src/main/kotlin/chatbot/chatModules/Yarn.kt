package chatbot.chatModules

import api.GarbageMessagesCollector
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
    private const val BASIC_USE_AMOUNT = 6
    private const val AMOUNT_MULTIPLIER = 2

    private var probability = 0
    @OnCommand(["нить", "yarn"], description = "Да найдите же ее кто-нибудь")
    fun yarn(event: LongPollNewMessageEvent) {
        val curCommandName = object : Any() {}.javaClass.enclosingMethod.name
        val canBeUsed = RatingSystem.canUseCommand(
            chatId = event.chatId,
            userId = event.userId,
            basicUseAmount = BASIC_USE_AMOUNT,
            amountMult = AMOUNT_MULTIPLIER,
            commandName = curCommandName
        )
        if (canBeUsed) {
            loseYarn(event)
        } else {
            event.api.send(
                "Товарищ, у вас закончились нитки. Обновление запаса ниток происходит раз в 4 часа",
                event.chatId,
                removeDelay = GarbageMessagesCollector.DEFAULT_DELAY
            )
        }
    }

    private fun loseYarn(event: LongPollNewMessageEvent) {
        event.api.send("Произвожу поиск...", event.chatId)
        probability++
        val delay = 3000L
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