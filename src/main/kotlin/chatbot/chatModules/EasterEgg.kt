package chatbot.chatModules

import api.GarbageMessage.Companion.toGarbageMessageWithDelay
import api.GarbageMessagesCollector
import api.GarbageMessagesCollector.Companion.DEFAULT_DELAY
import chatbot.ModuleObject
import chatbot.OnCommand
import chatbot.chatBotEvents.LongPollNewMessageEvent

@ModuleObject
object EasterEgg {
    private const val BASIC_USE_AMOUNT = 1
    private const val AMOUNT_MULTIPLIER = 1

    @OnCommand(["тождество"], showOnHelp = false)
    fun egg(event: LongPollNewMessageEvent) {
        val curCommandName = object : Any() {}.javaClass.enclosingMethod.name
        val canBeUsed = RatingSystem.canUseCommand(
            chatId = event.chatId,
            userId = event.userId,
            basicUseAmount = BASIC_USE_AMOUNT,
            amountMult = AMOUNT_MULTIPLIER,
            commandName = curCommandName
        )
        if (canBeUsed) {
            event.api.send("== != !=\nЕсли ты, конечно, знаешь, что такое !=", event.chatId, removeDelay = DEFAULT_DELAY)
        } else {
            event.api.send(
                "Товарищ, остановите спам (ограничения на спам обновляются раз в 4 часа)",
                event.chatId,
                removeDelay = DEFAULT_DELAY
            )
        }
        GarbageMessagesCollector.addGarbageMessage(event.toGarbageMessageWithDelay(0))
    }
}