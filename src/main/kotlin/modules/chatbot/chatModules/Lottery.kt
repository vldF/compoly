package modules.chatbot.chatModules

import api.TelegramPlatform
import modules.chatbot.ModuleObject
import modules.chatbot.OnCommand
import modules.chatbot.chatBotEvents.LongPollNewMessageEvent
import modules.chatbot.chatBotEvents.LongPollNewTGMessageEvent

@ExperimentalStdlibApi
@ModuleObject
class Lottery {
    @OnCommand(["\uD83C\uDFAF", "\uD83C\uDFB2", "⚽", "\uD83C\uDFC0"], cost = 40)
    fun play(event: LongPollNewMessageEvent) {
        if (event.api is TelegramPlatform)
        when((event as LongPollNewTGMessageEvent).diceResult) {
            1 -> sendResult(event,
                    "вы потеряли 40 е-баллов. Партия собалезнует вам",
                    0)
            2 -> sendResult(
                    event,
                    "вы потеряли 20 е-баллов",
                    10)
            3 -> sendResult(
                    event,
                    "вы потеряли 10 е-баллов",
                    30)
            4 -> sendResult(
                    event,
                    "вы ничего не получили (но и не потеряли)",
                    40)
            5 -> sendResult(
                    event,
                    "ваш выигрыш: 10 е-баллов",
                    50)
            6 -> sendResult(
                    event,
                    "ваш выигрыш: 20 е-баллов. Партия поздравляет вас",
                    60)
        }
    }

    private fun sendResult(event: LongPollNewMessageEvent, message: String, prize: Int) {
        val username = event.api.getUserNameById(event.userId)
        event.api.send("$username, $message", event.chatId)
        RatingSystem.addPoints(prize, event.userId, event.chatId, event.api)
    }
}