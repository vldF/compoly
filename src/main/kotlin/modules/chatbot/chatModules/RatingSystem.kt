package modules.chatbot.chatModules

import api.IntegerNumber
import api.Mention
import api.PlatformApiInterface
import api.TextMessageParser
import database.UserScore
import database.dbQuery
import log
import modules.chatbot.CommandPermission
import modules.chatbot.ModuleObject
import modules.chatbot.OnCommand
import modules.chatbot.OnMessage
import modules.chatbot.chatBotEvents.LongPollNewMessageEvent
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import kotlin.math.ceil
import kotlin.math.floor

@Suppress("DuplicatedCode")
@ModuleObject
object RatingSystem {
    private val regex = Regex("^(.+)\\s(.+)\\s(-?\\d+)\$")
    private val mentionRegex = Regex("id(\\d+)(.*)")
    private val respects = mutableMapOf<Pair<Long, Long>, Long>()
    private val disrespects = mutableMapOf<Pair<Long, Long>, Long>()

    private val levels = mapOf(
            10..110 to "октябрёнок",
            111..220 to "пионер",
            221..450 to "пролетарий",
            451..680 to "комсомолец",
            681..1400 to "член профсоюза",
            1401..2600 to "посол",
            2601..6000 to "генсек",
            6001..Integer.MAX_VALUE to "Гелич"
    )

    fun buyCommand(chatId: Long, userId: Long, cost: Int, api: PlatformApiInterface): Boolean {
        var canBuy = true
        dbQuery {
            val selected = UserScore.select {
                (UserScore.chatId eq chatId) and (UserScore.userId eq userId)
            }.firstOrNull()
            val senderPoints = selected?.get(UserScore.score) ?: 0
            if (senderPoints < cost) {
                canBuy = false
            }
        }

        if (canBuy) {
            addPoints(-cost, userId, chatId, api)
        }
        return canBuy
    }

    fun getLevelName(score: Int): String {
        for ((key, value) in levels) {
            if (score in key) return value
        }
        return "ЗАСЕКРЕЧЕНО"
    }

    fun addPoints(count: Int, toUser: Long, chat: Long, api: PlatformApiInterface) {
        var oldScore = -1
        var newScore = -1
        dbQuery {
            val selected = UserScore.select{
                (UserScore.chatId eq chat) and (UserScore.userId eq toUser)
            }.firstOrNull()
            if (selected == null) {
                UserScore.insert {
                    it[chatId] = chat
                    it[userId] = toUser
                    it[score] = count
                }
            } else {
                UserScore.update({ (UserScore.chatId eq chat) and (UserScore.userId eq toUser) }) {
                    it[score] = selected[score] +  count
                }
            }

            oldScore = selected?.get(UserScore.score) ?: 0
            newScore = count + oldScore
        }

        val level = getLevelName(newScore)
        val userName = api.getUserNameById(toUser)
        when {
            level != getLevelName(oldScore) && count > 0 -> {
                api.send("Партия поздравляет $userName с повышением до $level", chat)
            }
            level != getLevelName(oldScore) && count < 0 -> {
                api.send("Партия сочувствует ${userName}. Он понижен до $level", chat)
            }
        }
    }

    fun isUserHasScore(chatId: Long, userId: Long): Boolean {
        val selected = dbQuery {
            UserScore.select {
                (UserScore.chatId eq chatId) and (UserScore.userId eq userId)
            }.firstOrNull()
        }

        return selected != null
    }


    @OnCommand(
        ["добавить", "add"],
        "добавить пользователю очков. /add ID COUNT",
        CommandPermission.ADMIN_ONLY
    )
    fun add(event: LongPollNewMessageEvent) {
        val api = event.api
        val chatId = event.chatId
        val parsed = TextMessageParser(event.platform).parse(event.text)
        val target = parsed.get<Mention>(1)
        val targetId = target?.targetId
        try {
            val deltaScore = parsed.get<IntegerNumber>(2)?.number!!.toInt()
            if (target == null) {
                api.send("Не указан ссыльный", chatId)
                return
            }

            if (targetId == null) {
                log.info("arguments: $target, $deltaScore")
                api.send("Неверные аргументы, товарищ", chatId)
                return
            }
            val screenName = target.targetScreenName

            addPoints(deltaScore, targetId, chatId, api)
            if (deltaScore >= 0)
                api.send("Теперь у $screenName на $deltaScore e-балл больше!", chatId)
            else
                api.send("Теперь у $screenName на ${-deltaScore} e-балл меньше!", chatId)

        } catch (e: NumberFormatException) {
            api.send("Некорректное число, товарищ", chatId)
            return
        }
    }

    @OnMessage
    fun onMessageReceive(event: LongPollNewMessageEvent) {
        val api = event.api
        val chatId = event.chatId
        val count = when (event.text.split(" ").filter { it.length > 2 }.size) {
            in 0..1 -> 0
            in 2..6 -> 1
            in 7..10 -> 2
            in 11..20 -> 3
            in 21..50 -> 4
            else -> 5
        }

        addPoints(count, event.userId, chatId, api)
    }


    @OnCommand(["уровень", "level", "lvl"], "посмотреть количество e-баллов")
    fun showUsersInfo(event: LongPollNewMessageEvent) {
        val api = event.api
        val chatId = event.chatId
        val userId = event.userId

        val score = dbQuery {
            UserScore.select{
                (UserScore.chatId eq chatId) and (UserScore.userId eq userId)
            }.firstOrNull()?.get(UserScore.score) ?: 0
        }

        val levelName = getLevelName(score)
        val screenName = api.getUserNameById(userId)

        val showedScore = "$score".let {
            val l = it.length * 1.0 - 1
            it.substring(0..floor(l/2).toInt()) + "0".repeat(ceil(l/2).toInt())
        }

        api.send("По архивам Партии, у $screenName уровень $levelName. Это примерно $showedScore e-баллов", chatId)
    }

    @OnCommand(["одобряю", "респект", "respect"],
            "показать одобрение и подкинуть чуть-чуть e-баллов. /одобряю ОДОБРЯЕМЫЙ")
    fun respect(event: LongPollNewMessageEvent) {
        val api = event.api
        val peerId = event.chatId
        val sender = event.userId
        val parsed = TextMessageParser(event.platform).parse(event.text)
        if (parsed.size < 2) {
            api.send("Не указан одобряемый", peerId)
            return
        }

        val target = parsed.get<Mention>(1)
        val targetId = target?.targetId
        if (targetId == null || !isUserHasScore(peerId, targetId)) {
            api.send("Партии неизвестно это лицо", peerId)
            return
        }

        if (targetId == sender) {
            api.send("Партия рекомендует не удалять рёбра", peerId)
            return
        }

        val currentTime = System.currentTimeMillis()
        if (
                respects[sender to peerId] != null &&
                currentTime - respects[sender to peerId]!! < 1000 * 60 * 60 * 4
        ) {
            api.send("Партия не рекомендует одобрение других лиц чаще, чем раз в 4 часа", peerId)
            return
        }

        respects[sender to peerId] = currentTime
        addPoints(10, targetId, peerId, api)
        api.send("Одобрение выражено", peerId)
    }

    @OnCommand(["осуждаю"], "показать осуждение и убрать чуть-чуть e-баллов. /осуждаю ОСУЖДАЕМЫЙ")
    fun disrespect(event: LongPollNewMessageEvent) {
        val api = event.api
        val peerId = event.chatId
        val sender = event.userId
        val parsed = TextMessageParser(event.platform).parse(event.text)
        if (parsed.size < 2) {
            api.send("Не указан одобряемый", peerId)
            return
        }

        val target = parsed.get<Mention>(1)
        val targetId = target?.targetId
        if (targetId == null || !isUserHasScore(peerId, targetId)) {
            api.send("Партии неизвестно это лицо", peerId)
            return
        }

        if (targetId == sender) {
            api.send("Партия рекомендует не удалять рёбра", peerId)
            return
        }

        val currentTime = System.currentTimeMillis()
        if (
                disrespects[sender to peerId] != null &&
                currentTime - disrespects[sender to peerId]!! < 1000 * 60 * 60 * 4
        ) {
            api.send("Партия не рекомендует осуждение других лиц чаще, чем раз в 4 часа", peerId)
            return
        }

        disrespects[sender to peerId] = currentTime
        addPoints(-10, targetId, peerId, api)
        api.send("Осуждение выражено", peerId)
    }
}