package modules.chatbot.chatModules

import api.PlatformApiInterface
import api.TelegramPlatform
import modules.chatbot.*
import modules.chatbot.chatBotEvents.LongPollNewMessageEvent
import modules.chatbot.chatBotEvents.LongPollNewPollAnswerEvent
import modules.chatbot.chatBotEvents.LongPollNewTGMessageEvent
import java.util.*
import kotlin.random.Random

@ExperimentalStdlibApi
@ModuleObject
object CapitalismGame {
    private var capitals = mutableMapOf<String, Int>()
    private var answer: Int = -1
    private val winnersIds = mutableMapOf<Long, MutableSet<Long>>()
    private val chatIds = mutableMapOf<String, Long>()

    @OnCommand(["startGame"], cost = 0)
    fun startGame(event: LongPollNewMessageEvent) {
        if (chatIds.containsValue(event.chatId) || event.api !is TelegramPlatform) return
        val telegram = event.api as TelegramPlatform
        answer = 3
        val durationInSec = 3
        val gameId = telegram.sendPoll(
                event.chatId,
                "Choose your destiny",
                Array(10) { i -> i.toString() },
                answer,
                System.currentTimeMillis() / 1000 + durationInSec,
                "quiz",
                false
        )
        if (gameId == null) {
            answer = -1
            return
        }
        chatIds[gameId] = event.chatId
        winnersIds[event.chatId] = mutableSetOf()
        capitals[gameId] = 40

        Timer().schedule(
                object : TimerTask() {
                    override fun run() {
                        stopGame(event.chatId, gameId, event.api)
                    }
                },
                durationInSec * 1000L
        )
    }

    @OnPollAnswer
    fun processPollAnswer(event: LongPollNewPollAnswerEvent) {
        if (!chatIds.containsKey(event.pollId)) return
        RatingSystem.addPoints(-5, event.userId, chatIds[event.pollId]!!, event.api)
        capitals[event.pollId] = capitals[event.pollId]!! + 10
        if (event.optionIds.contains(answer))
            winnersIds[chatIds[event.pollId]!!]!!.add(event.userId)
    }

    fun stopGame(chatId: Long, pollId: String, api: PlatformApiInterface) {
        val prize = capitals[pollId]!! / winnersIds[chatId]!!.size
        for (userId in winnersIds[chatId]!!) {
            RatingSystem.addPoints(prize, userId, chatId, api)
        }
        val winnerNames = mutableListOf<String>()
        for(userId in winnersIds[chatId]!!) {
            val userName = api.getUserNameById(userId)
            if (userName != null )winnerNames.add("@$userName")
        }
        if (!winnerNames.isEmpty()) api.send("our winners: $winnerNames", chatId)
        else api.send("we have no winners", chatId)
        winnersIds.remove(chatId)
        chatIds.remove(pollId)
    }
}