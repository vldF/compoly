package chatbot.chatModules

import api.IntegerNumber
import api.Mention
import api.TextMessageParser
import api.VkPlatform
import database.UserScore
import database.dbQuery
import log
import chatbot.CommandPermission
import chatbot.ModuleObject
import chatbot.OnCommand
import chatbot.OnMessage
import chatbot.chatBotEvents.LongPollNewMessageEvent
import database.EMPTY_HISTORY_TEXT
import database.UserReward
import org.jetbrains.exposed.sql.*
import java.lang.IllegalArgumentException

@Suppress("DuplicatedCode")
@ModuleObject
object RatingSystem {
    private val respects = mutableMapOf<Pair<Int, Int>, Long>()
    private val disrespects = mutableMapOf<Pair<Int, Int>, Long>()

    enum class Level(val levelName: String) {
        LEVEL0("ЗАСЕКРЕЧЕНО"),
        LEVEL1("октябрёнок"),
        LEVEL2("пионер"),
        LEVEL3("пролетарий"),
        LEVEL4("комсомолец"),
        LEVEL5("член профсоюза"),
        LEVEL6("посол"),
        LEVEL7("генсек"),
        LEVEL8("Гелич");

        companion object {
            fun getLevel(rep: Int): Level {
                for ((key, value) in levels) {
                    if (rep in key) return value
                }
                throw IllegalArgumentException("Can't find proper level in level map")
            }
        }
    }

    private val levels = mapOf(
            Integer.MIN_VALUE..-1 to Level.LEVEL0,
            0..20 to Level.LEVEL1,
            21..50 to Level.LEVEL2,
            51..100 to Level.LEVEL3,
            101..200 to Level.LEVEL4,
            201..500 to Level.LEVEL5,
            501..1000 to Level.LEVEL6,
            1001..5000 to Level.LEVEL7,
            5001..Integer.MAX_VALUE to Level.LEVEL8
    )

    private fun addReputation(count: Int, toUser: Int, chatId: Int, shadowChatId: Int, api: VkPlatform) {
        var oldRep = -1
        var newRep = -1
        dbQuery {
            val selected = UserScore.select{
                (UserScore.chatId eq shadowChatId) and (UserScore.userId eq toUser)
            }.firstOrNull()
            if (selected == null) {
                UserScore.insert {
                    it[this.chatId] = shadowChatId
                    it[userId] = toUser
                    it[reputation] = count
                    it[history_respects] = EMPTY_HISTORY_TEXT
                    it[history_disrespects] = EMPTY_HISTORY_TEXT
                }
            } else {
                UserScore.update({ (UserScore.chatId eq shadowChatId) and (UserScore.userId eq toUser) }) {
                    it[reputation] = selected[reputation] +  count
                }
            }

            oldRep = selected?.get(UserScore.reputation) ?: 0
            newRep = count + oldRep
        }

        val levelName = Level.getLevel(newRep).levelName
        val userName = api.getUserNameById(toUser)
        when {
            levelName != Level.getLevel(oldRep).levelName && count > 0 -> {
                api.send("Партия поздравляет $userName с повышением до $levelName", chatId)
            }
            levelName != Level.getLevel(oldRep).levelName && count < 0 -> {
                api.send("Партия сочувствует ${userName}. Он понижен до $levelName", chatId)
            }
        }
    }

    private fun userHasScore(chatId: Int, userId: Int): Boolean {
        val selected = dbQuery {
            UserScore.select {
                (UserScore.chatId eq chatId) and (UserScore.userId eq userId)
            }.firstOrNull()
        }

        return selected != null
    }

    @OnCommand(
        ["add", "добавить"],
        "добавить пользователю репы. /add ID COUNT",
        CommandPermission.ADMIN
    )
    fun addRep(event: LongPollNewMessageEvent) {
        val api = event.api
        val chatId = event.chatId
        val parsed = TextMessageParser().parse(event.text)
        val target = parsed.get<Mention>(1)
        val targetId = target?.targetId
        try {
            val deltaRep = parsed.get<IntegerNumber>(2)?.number!!.toInt()
            if (target == null) {
                api.send("Не указан товарищ", chatId)
                return
            }

            if (targetId == null) {
                log.info("arguments: $target, $deltaRep")
                api.send("Неверные аргументы, товарищ", chatId)
                return
            }
            val screenName = target.targetScreenName
            addReputation(deltaRep, targetId, chatId, chatId, api)

            if (deltaRep >= 0)
                api.send("Теперь у $screenName на $deltaRep реп больше!", chatId)
            else
                api.send("Теперь у $screenName на ${-deltaRep} реп меньше!", chatId)

        } catch (e: NumberFormatException) {
            api.send("Некорректное число, товарищ", chatId)
            return
        }
    }

    @OnMessage
    fun onMessageReceive(event: LongPollNewMessageEvent) {
        val toUser = event.userId
        val chatId = event.chatId
        dbQuery {
            val selected = UserScore.select{
                (UserScore.chatId eq chatId) and (UserScore.userId eq toUser)
            }.firstOrNull()

            if (selected == null) {
                UserScore.insert {
                    it[this.chatId] = chatId
                    it[userId] = toUser
                    it[reputation] = 0
                    it[history_respects] = EMPTY_HISTORY_TEXT
                    it[history_disrespects] = EMPTY_HISTORY_TEXT
                }
            }
        }
    }

    @OnCommand(["уровень", "level", "lvl"], "посмотреть уровень")
    fun showUsersInfo(event: LongPollNewMessageEvent) {
        val api = event.api
        val chatId = event.chatId
        val userId = event.userId

        val rep = dbQuery {
            UserScore.select{
                (UserScore.chatId eq chatId) and (UserScore.userId eq userId)
            }.firstOrNull()?.get(UserScore.reputation) ?: 0
        }

        val levelName = Level.getLevel(rep).levelName
        val screenName = api.getUserNameById(userId)

        dbQuery {
            val rowList = UserReward.select {
                (UserReward.chatId eq chatId) and (UserReward.userId eq userId)
            }.toList()
            if (rowList.isNotEmpty()) {
                val rewardsList = rowList.map { it[UserReward.rewardName] }
                val rewardsStr = rewardsList.joinToString(separator = ", ")
                api.send("По архивам Партии, у $screenName уровень $levelName и награды: $rewardsStr", chatId)
            } else {
                api.send("По архивам Партии, у $screenName уровень $levelName", chatId)
            }
        }
    }

    @OnCommand(["одобряю", "респект", "respect"],
            "показать одобрение и повысить репутацию. /одобряю ОДОБРЯЕМЫЙ")
    fun respect(event: LongPollNewMessageEvent) {
        val api = event.api
        val chatId = event.chatId
        val senderId = event.userId
        val parsed = TextMessageParser().parse(event.text)

        val target = parsed.get<Mention>(1)
        var targetId = target?.targetId

        if (targetId == null) {
            val forwardedFrom = event.forwardMessageFromId
            if (forwardedFrom != null) {
                targetId = forwardedFrom
            } else {
                api.send("Укажите одобряемого", chatId)
                return
            }
        }

        if (targetId == api.meId || targetId == -api.meId) {
            api.send("Мы и так знаем, что Вы, Товарищ, одобряете Нас!", chatId)
            return
        }

        if (!userHasScore(chatId, targetId)) {
            api.send("Этого человека нет в архивах", chatId)
            return
        }

        if (targetId == senderId) {
            api.send("Партия рекомендует не удалять рёбра", chatId)
            return
        }

        val currentTime = System.currentTimeMillis()
        if (
                respects[senderId to chatId] != null &&
                currentTime - respects[senderId to chatId]!! < 1000 * 60 * 60 * 4
        ) {
            val timeLeft = (1000 * 60 * 60 * 4 + respects[senderId to chatId]!! - currentTime) / 1000
            val coolDown = String.format("%d:%02d:%02d", timeLeft / 3600, timeLeft % 3600 / 60, timeLeft % 3600 % 60)
            api.send(
                "Партия не рекомендует одобрение других лиц чаще, чем раз в 4 часа.\nСледующее одобрение будет доступно через: $coolDown",
                chatId
            )
            return
        }

        respects[senderId to chatId] = currentTime
        val count = calculateRep(targetId, chatId, event, RepCommandType.RESPECT)
        addReputation(count, targetId, chatId, chatId, api)
        updateHistory(chatId, senderId, targetId, RepCommandType.RESPECT)

        api.send("Одобрение выражено", chatId)
    }

    private fun calculateRep(
        targetId: Int,
        shadowChatId: Int,
        event: LongPollNewMessageEvent,
        commandType: RepCommandType
    ): Int {
        val isRespect = commandType == RepCommandType.RESPECT
        val dbField = if (isRespect) UserScore.history_respects else UserScore.history_disrespects
        val historyTxt = dbQuery {
            UserScore.select{
                (UserScore.chatId eq shadowChatId) and (UserScore.userId eq event.userId)
            }.firstOrNull()?.get(dbField)
        }

        val baseCount = 10
        return if (historyTxt != null && historyTxt != "") {
            val historyList = historyTxt.split(',')
            val historySize = Math.min(historyList.size, 10)
            val subList = historyList.subList(0, historySize)
            val repeatCount = subList.filter { it.toInt() == targetId }.size
            val count = Math.max(baseCount - repeatCount, 0)
            if (isRespect) count else -count
        } else {
            if (isRespect) baseCount  else -baseCount
        }
    }

    private fun updateHistory(chatId: Int, sender: Int, targetId: Int, commandType: RepCommandType) {
        val historyColumn =
            if(commandType == RepCommandType.RESPECT) UserScore.history_respects
            else UserScore.history_disrespects
        dbQuery {
            val selected = UserScore.select{
                (UserScore.chatId eq chatId) and (UserScore.userId eq sender)
            }.first()
            UserScore.update({ (UserScore.chatId eq chatId) and (UserScore.userId eq sender) }) {
                val selectedHistory = selected.getOrNull(historyColumn)
                if (selectedHistory == EMPTY_HISTORY_TEXT) {
                    it[historyColumn] = "$targetId"
                } else {
                    it[historyColumn] = "${selected[historyColumn]},$targetId"
                }
            }
        }
    }

    private enum class RepCommandType {
        RESPECT, DISRESPECT
    }

    @OnCommand(["осуждаю", "disrespect"], "показать осуждение и понизить репутацию. /осуждаю ОСУЖДАЕМЫЙ")
    fun disrespect(event: LongPollNewMessageEvent) {
        val api = event.api
        val chatId = event.chatId
        val senderId = event.userId
        val parsed = TextMessageParser().parse(event.text)

        val target = parsed.get<Mention>(1)
        var targetId = target?.targetId
        if (targetId == null) {
            val forwardedFrom = event.forwardMessageFromId
            if (forwardedFrom != null) {
                targetId = forwardedFrom
            } else {
                api.send("Укажите осуждаемого", chatId)
                return
            }
        }

        if (targetId == api.meId || targetId == -api.meId) {
            api.send("Отправляю чёрных воронков", chatId)
            return
        }

        if (!userHasScore(chatId, targetId)) {
            api.send("Этого человека нет в архивах", chatId)
            return
        }

        if (targetId == senderId) {
            api.send("Партия рекомендует не удалять рёбра", chatId)
            return
        }

        val currentTime = System.currentTimeMillis()
        if (
                disrespects[senderId to chatId] != null &&
                currentTime - disrespects[senderId to chatId]!! < 1000 * 60 * 60 * 4
        ) {
            val timeLeft = (1000 * 60 * 60 * 4 + disrespects[senderId to chatId]!! - currentTime) / 1000
            val coolDown = String.format("%d:%02d:%02d", timeLeft / 3600, timeLeft % 3600 / 60, timeLeft % 3600 % 60)
            api.send(
                "Партия не рекомендует осуждение других лиц чаще, чем раз в 4 часа.\nСледующее осуждение будет доступно через: $coolDown",
                chatId
            )
            return
        }

        disrespects[senderId to chatId] = currentTime
        val count = calculateRep(targetId, chatId, event, RepCommandType.DISRESPECT)
        addReputation(count, targetId, chatId, chatId, api)
        updateHistory(chatId, senderId, targetId, RepCommandType.DISRESPECT)

        api.send("Осуждение выражено", chatId)
    }

    //метод для тестов
    @OnCommand(["showRespectHistory"], "вскрываем историю одобрений", CommandPermission.ADMIN)
    fun showRespectHistory(event: LongPollNewMessageEvent) {
        val api = event.api
        val parsed = TextMessageParser().parse(event.text)
        val target = parsed.get<Mention>(1)
        val shadowChatId = event.chatId
        if (target == null) {
            api.send("Не указан интересущий член партии", shadowChatId)
            return
        }
        var targetId = target.targetId ?: throw IllegalArgumentException("Target hasn't ID")

        val respectHistoryTxt = dbQuery {
            UserScore.select{
                (UserScore.chatId eq shadowChatId) and (UserScore.userId eq targetId)
            }.firstOrNull()?.get(UserScore.history_respects).toString()
        }
        api.send(respectHistoryTxt, shadowChatId)
    }
}
