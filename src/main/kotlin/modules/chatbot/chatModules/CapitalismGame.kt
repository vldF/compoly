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
    private val capitals = mutableMapOf<String, Int>()
    private const val price = 10
    private const val cooldownInSec = 3600 * 3
    private var answer = -1
    //todo: надо бы придумать как лучше организовать хранение всех этих id и времени
    private val winnersIds = mutableMapOf<Long, MutableSet<Long>>()
    private val losersIds = mutableMapOf<Long, MutableSet<Long>>()
    private val chatIds = mutableMapOf<String, Long>()
    private val chatsCooldown = mutableMapOf<Long, Long>()

    @OnCommand(["капитализм", "capitalism"], cost = 20, description = "Выбери правильный ответ или отдай e-баллы побеителю")
    fun startGame(event: LongPollNewMessageEvent) {
        val currentTime = System.currentTimeMillis() / 1000
        if (event.api !is TelegramPlatform) {
            event.api.send(
                    "Товарищ, данная платворма не предназначена для игр",
                    event.chatId
            )
            return
        }
        if(chatIds.containsValue(event.chatId)) {
            event.api.send(
                    "Товарищ, игра уже идет",
                    event.chatId
            )
            return
        }
        if (chatsCooldown[event.chatId] ?: 0 > currentTime) {
            event.api.send(
                    "Партия не рекомендует отвлекаться от работы более чем раз в час",
                    event.chatId
            )
            return
        }
        val telegram = event.api as TelegramPlatform
        answer = Random.nextInt(0, 9)
        val durationInSec = 600
        val gameId = telegram.sendPoll(
                event.chatId,
                "Choose your destiny",
                Array(10) { i -> i.toString() },
                answer,
                currentTime + durationInSec,
                "quiz",
                false
        )
        if (gameId == null) {
            answer = -1
            return
        }
        chatIds[gameId] = event.chatId
        winnersIds[event.chatId] = mutableSetOf()
        losersIds[event.chatId] = mutableSetOf()
        capitals[gameId] = 40

        Timer().schedule(
                object : TimerTask() {
                    override fun run() {
                        stopGame(event.chatId, gameId, event.api)
                    }
                },
                durationInSec * 1000L + 10
        )

        chatsCooldown[event.chatId] = currentTime + cooldownInSec
    }

    @OnPollAnswer
    fun processPollAnswer(event: LongPollNewPollAnswerEvent) {
        if (!chatIds.containsKey(event.pollId)) return
        RatingSystem.addPoints(-price, event.userId, chatIds[event.pollId]!!, event.api)
        capitals[event.pollId] = capitals[event.pollId]!! + 10
        if (event.optionIds.contains(answer))
            winnersIds[chatIds[event.pollId]!!]!!.add(event.userId)
        else  losersIds[chatIds[event.pollId]!!]!!.add(event.userId)
        val  a = losersIds.isEmpty()
    }

    fun stopGame(chatId: Long, pollId: String, api: PlatformApiInterface) {
        if (winnersIds[chatId]!!.isNotEmpty()) {
            val winnersNames = mutableListOf<String>()
            val prize = capitals[pollId]!! / winnersIds[chatId]!!.size
            for(userId in winnersIds[chatId]!!) {
                RatingSystem.addPoints(prize, userId, chatId, api)
                val userName = api.getUserNameById(userId)
                if (userName != null ) winnersNames.add("@$userName")
            }
            api.send("Наши победители: ${winnersNames.joinToString(", ")}", chatId)
        }
        else {
            val losersNames = mutableListOf<String>()
            for(userId in losersIds[chatId]!!) {
                RatingSystem.addPoints(price, userId, chatId, api)
                val userName = api.getUserNameById(userId)
                if (userName != null ) losersNames.add("@$userName")
            }
            api.send("Победителей нет, капитал был возвращен пролетариям", chatId)
        }
        winnersIds.remove(chatId)
        chatIds.remove(pollId)
    }
}