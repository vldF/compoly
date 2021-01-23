package chatbot.chatModules

import chatbot.ModuleObject
import chatbot.OnCommand
import chatbot.chatBotEvents.LongPollNewMessageEvent
import kotlin.random.Random

@ExperimentalStdlibApi
@ModuleObject
object Lottery {
    @OnCommand(
            [
                "\uD83C\uDFAF",
                "\uD83C\uDFB2",
                "⚽",
                "\uD83C\uDFC0",
                "lottery",
                "лотерея"
            ],
            cost = 0
    )
    fun play(event: LongPollNewMessageEvent) {
        when(Random.nextInt(1, 6)) {
            1 -> sendResult(event,
                    "вы потеряли 40 е-баллов. Партия соболезнует вам",
                    -40)
            2 -> sendResult(
                    event,
                    "вы потеряли 20 е-баллов",
                    -20)
            3 -> sendResult(
                    event,
                    "вы потеряли 10 е-баллов",
                    -10)
            4 -> sendResult(
                    event,
                    "вы ничего не получили (но и не потеряли)",
                    0)
            5 -> sendResult(
                    event,
                    "ваш выигрыш: 10 е-баллов",
                    10)
            6 -> sendResult(
                    event,
                    "ваш выигрыш: 20 е-баллов. Партия поздравляет вас",
                    20)
        }
    }

    private fun sendResult(event: LongPollNewMessageEvent, message: String, prize: Int) {
        val username = event.api.getUserNameById(event.userId)
        event.api.send("$username, $message", event.chatId)
    }
}