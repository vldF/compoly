package modules.chatbot.chatModules

import api.Vk
import database.UserScore
import database.dbQuery
import log
import modules.Active
import modules.chatbot.CommandPermission
import modules.chatbot.MessageNewObj
import modules.chatbot.OnCommand
import modules.chatbot.OnMessage
import org.jetbrains.exposed.sql.*
import kotlin.math.ceil
import kotlin.math.floor

@Suppress("DuplicatedCode")
@Active
class RatingSystem {
    private val regex = Regex("^(.+)\\s(.+)\\s(-?\\d+)\$")
    private val mentionRegex = Regex("id(\\d+)(.*)")
    private val respects = mutableMapOf<Pair<Int, Int>, Long>()
    private val disrespects = mutableMapOf<Pair<Int, Int>, Long>()

    companion object {
        private val vk = Vk()

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

        fun buyCommand(chatId: Int, userId: Int, cost: Int): Boolean {
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
                addPoints(-cost, userId, chatId)
            }
            return canBuy
        }

        fun getLevelName(score: Int): String {
            for ((key, value) in levels) {
                if (score in key) return value
            }
            return "ЗАСЕКРЕЧЕНО"
        }

        fun addPoints(count: Int, toUser: Int, chat: Int) {
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

            val name = getLevelName(newScore)
            if (name != getLevelName(oldScore)) {
                val screenName = vk.getUserDisplayName(toUser)
                vk.send("Партия поздравляет $screenName с повышением до $name!", chat)
            }
        }

        fun isUserHasScore(chatId: Int, userId: Int): Boolean {
            val selected = dbQuery {
                UserScore.select {
                    (UserScore.chatId eq chatId) and (UserScore.userId eq userId)
                }.firstOrNull()
            }

            return selected != null
        }
    }


    @OnCommand(
        ["добавить", "add"],
        "добавить пользователю очков. /add ID COUNT",
        CommandPermission.ADMIN_ONLY
    )
    fun add(messageObj: MessageNewObj) {
        val peerId = messageObj.peer_id
        val messageParts = regex.find(messageObj.text)
        val target = messageParts?.groupValues?.get(2)
        val deltaScore = messageParts?.groupValues?.get(3)?.let {
            Integer.parseInt(it)
        }

        val targetId = target?.let {
            if (it.contains("id"))
                mentionRegex.find(it)?.groupValues?.get(1).let { v -> Integer.parseInt(v) }
            else {
                val name = when {
                    it.contains("vk.com/") -> it.split("vk.com/")[1]
                    it.startsWith("@") -> it.removePrefix("@")
                    else -> it
                }
                vk.getUserId(name)
            }
        }
            if (
                messageParts?.groupValues == null ||
                messageParts.groupValues.size != 4 ||
                target == null ||
                targetId == null ||
                deltaScore == null
            ) {
                log.info("arguments: $target, $deltaScore")
                vk.send("Неверные аргументы, товарищ", peerId)
                return
        }

        addPoints(deltaScore, targetId, peerId)
        if (deltaScore >= 0)
            vk.send("Теперь у $target на $deltaScore e-балл больше!", messageObj.peer_id)
        else
            vk.send("Теперь у $target на ${-deltaScore} e-балл меньше!", messageObj.peer_id)
    }

    @OnMessage
    fun onMessageReceive(message: MessageNewObj) {
        val userId = message.from_id
        val chatId = message.peer_id

        val count = when (message.text.split(" ").filter { it.length > 2 }.size) {
            in 0..1 -> 0
            in 2..6 -> 1
            in 7..10 -> 2
            in 11..20 -> 3
            in 21..50 -> 4
            else -> 5
        }

        addPoints(count, userId, chatId)
    }


    @OnCommand(["уровень", "level", "lvl"], "посмотреть количество e-баллов")
    fun showUsersInfo(messageObj: MessageNewObj) {
        val userId = messageObj.from_id
        val chatId = messageObj.peer_id
        val score = dbQuery {
            UserScore.select{
                (UserScore.chatId eq chatId) and (UserScore.userId eq userId)
            }.firstOrNull()?.get(UserScore.score) ?: 0
        }

        val levelName = getLevelName(score)
        val screenName = vk.getUserDisplayName(userId)

        val showedScore = "$score".let {
            val l = it.length * 1.0 - 1
            it.substring(0..floor(l/2).toInt()) + "0".repeat(ceil(l/2).toInt())
        }

        vk.send("По архивам Партии, у $screenName уровень $levelName. Это примерно $showedScore e-баллов", chatId)
    }

    @OnCommand(["одобряю", "респект", "respect"],
            "показать одобрение и подкинуть чуть-чуть e-баллов. /одобряю ОДОБРЯЕМЫЙ")
    fun respect(messageObj: MessageNewObj) {
        val peerId = messageObj.peer_id
        val sender = messageObj.from_id
        val parts = messageObj.text.split(" ")
        if (parts.size < 2) {
            vk.send("Не указан одобряемый", peerId)
            return
        }

        val target = parts[1]
        val targetId = target.let {
            if (it.contains("[id"))
                mentionRegex.find(it)?.groupValues?.get(1)?.let { v -> Integer.parseInt(v) }
            else {
                val name = when {
                    it.contains("vk.com/") -> it.split("vk.com/")[1]
                    it.startsWith("@") -> it.removePrefix("@")
                    else -> it
                }
                vk.getUserId(name)
            }
        }

        if (targetId == null || !isUserHasScore(peerId, targetId)) {
            vk.send("Партии неизвестно это лицо", peerId)
            return
        }

        if (targetId == sender) {
            vk.send("Партия рекомендует не удалять рёбра", peerId)
            return
        }

        val currentTime = System.currentTimeMillis()
        if (
                respects[sender to peerId] != null &&
                currentTime - respects[sender to peerId]!! < 1000 * 60 * 60 * 4
        ) {
            vk.send("Партия не рекомендует одобрение других лиц чаще, чем раз в 4 часа", peerId)
            return
        }

        respects[sender to peerId] = currentTime
        addPoints(10, targetId, peerId)
        vk.send("Одобрение выражено", peerId)
    }

    @OnCommand(["осуждаю"], "показать осуждение и убрать чуть-чуть e-баллов. /осуждаю ОСУЖДАЕМЫЙ")
    fun disrespect(messageObj: MessageNewObj) {
        val peerId = messageObj.peer_id
        val sender = messageObj.from_id
        val parts = messageObj.text.split(" ")
        if (parts.size < 2) {
            vk.send("Не указан осуждаемый", peerId)
            return
        }

        val target = parts[1]
        val targetId = target.let {
            if (it.contains("[id"))
                mentionRegex.find(it)?.groupValues?.get(1)?.let { v -> Integer.parseInt(v) }
            else {
                val name = when {
                    it.contains("vk.com/") -> it.split("vk.com/")[1]
                    it.startsWith("@") -> it.removePrefix("@")
                    else -> it
                }
                vk.getUserId(name)
            }
        }

        if (targetId == null || !isUserHasScore(peerId, targetId)) {
            vk.send("Партии неизвестно это лицо", peerId)
            return
        }

        if (targetId == sender) {
            vk.send("Партия рекомендует не удалять рёбра", peerId)
            return
        }

        val currentTime = System.currentTimeMillis()
        if (
                disrespects[sender to peerId] != null &&
                currentTime - disrespects[sender to peerId]!! < 1000 * 60 * 60 * 4
        ) {
            vk.send("Партия не рекомендует осуждение других лиц чаще, чем раз в 4 часа", peerId)
            return
        }

        disrespects[sender to peerId] = currentTime
        addPoints(-10, targetId, peerId)
        vk.send("Осуждение выражено", peerId)
    }
}