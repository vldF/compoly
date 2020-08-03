package modules.chatbot.chatModules

import api.TelegramPlatform
import modules.chatbot.ModuleObject
import modules.chatbot.OnCommand
import modules.chatbot.chatBotEvents.LongPollNewMessageEvent

@ExperimentalStdlibApi
@ModuleObject
class Lottery {
    @OnCommand(["lottery", "darts", "дартс"], "Дартс, дартс, дартс, дартс", cost = 20)
    fun play(event: LongPollNewMessageEvent) {
        if (event.api !is TelegramPlatform) return
        val telegram = event.api
        when(telegram.sendDice(event.chatId)
            ) {
            1 -> sendResult(event,
                    "вы потеряли 20 е-баллов. Партия собалезнует вам",
                    0)
            2 -> sendResult(
                    event,
                    "вы потеряли 10 е-баллов",
                    10)
            3 -> sendResult(
                    event,
                    "вы ничего не получили (но и не потеряли)",
                    20)
            4 -> sendResult(
                    event,
                    "ваш выигрыш: 10 е-баллов",
                    30)
            5 -> sendResult(
                    event,
                    "ваш выигрыш: 20 е-баллов",
                    40)
            6 -> sendResult(
                    event,
                    "ваш выигрыш: 30 е-баллов. Партия поздравляет вас",
                    50)
        }
    }

    private fun sendResult(event: LongPollNewMessageEvent, message: String, prize: Int) {
        val username = event.api.getUserNameById(event.userId)
        event.api.send("$username, $message", event.chatId)
        RatingSystem.addPoints(prize, event.userId, event.chatId, event.api)
    }
}