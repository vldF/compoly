package chatbot.chatModules

import api.GarbageMessage.Companion.toGarbageMessageWithDelay
import api.GarbageMessagesCollector
import api.GarbageMessagesCollector.Companion.DEFAULT_DELAY
import chatbot.ModuleObject
import chatbot.OnCommand
import chatbot.UsageInfo
import chatbot.chatBotEvents.LongPollNewMessageEvent

@ModuleObject
object EasterEgg {
    private const val notEnoughMessage = "Товарищ, ваши пасхалки закончились. Обновление запаса пасхалок происходит раз в 12 часов"

    @UsageInfo(baseUsageAmount = 1, levelBonus = 1, notEnoughMessage)
    @OnCommand(["тождество"], showInHelp = false)
    fun egg(event: LongPollNewMessageEvent) {
        event.api.send("== != !=\nЕсли ты, конечно, знаешь, что такое !=", event.chatId, removeDelay = DEFAULT_DELAY)
        GarbageMessagesCollector.addGarbageMessage(event.toGarbageMessageWithDelay(0))
    }
}