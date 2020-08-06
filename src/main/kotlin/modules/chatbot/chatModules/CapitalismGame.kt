package modules.chatbot.chatModules

import api.TelegramPlatform
import modules.chatbot.*
import modules.chatbot.chatBotEvents.LongPollNewMessageEvent
import modules.chatbot.chatBotEvents.LongPollNewPollAnswerEvent
import modules.chatbot.chatBotEvents.LongPollNewTGMessageEvent
import kotlin.random.Random

@ExperimentalStdlibApi
@ModuleObject
object CapitalismGame {
    private var capital = 0
    private var answer: Int = -1
    private val winnersIds = mutableMapOf<Long, MutableSet<Long>>()
    private val chatIds = mutableMapOf<String, Long>()

    @OnCommand(["startGame"], cost = 0)
    fun startGame(event: LongPollNewMessageEvent) {
        if (chatIds.containsValue(event.chatId) || event.api !is TelegramPlatform) return
        val telegram = event.api as TelegramPlatform
        answer = Random.nextInt(0, 9)
        val gameId = telegram.sendPoll(
                event.chatId,
                "Choose your destiny",
                Array(10) { i -> i.toString() },
                answer,
                System.currentTimeMillis() / 1000 + 10,
                "quiz"
        )
        if (gameId == null) {
            answer = -1
            return
        }
        chatIds[gameId] = event.chatId
        winnersIds[event.chatId] = mutableSetOf()
    }

    @OnPollAnswer
    fun processPollAnswer(event: LongPollNewPollAnswerEvent) {
        if (!chatIds.containsKey(event.pollId)) return
        RatingSystem.addPoints(-5, event.userId, chatIds[event.pollId]!!, event.api)
        capital += 10
        if(event.optionIds.contains(answer))
            winnersIds[chatIds[event.pollId]!!]!!.add(event.userId)
    }

    @OnPoll
    fun stopGame(event: LongPollNewMessageEvent) {
        if (
                event !is LongPollNewTGMessageEvent
                || !chatIds.containsValue(event.chatId)
        ) return
        event.api.send("stopGame", event.chatId)
        val prize = capital / winnersIds[event.chatId]!!.size
        for (userId in winnersIds[event.chatId]!!) {
            RatingSystem.addPoints(prize, event.userId, event.chatId, event.api)
        }
        winnersIds.remove(event.chatId)
        chatIds.remove(event.pollId)
    }
}