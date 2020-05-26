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

@Suppress("DuplicatedCode")
@Active
class RatingSystem {
    private val regex = Regex("^(.+)\\s(.+)\\s(-?\\d+)\$")
    private val mentionRegex = Regex("id(\\d+)(.*)")
    private val vk = Vk()
    private val respects = mutableMapOf<Pair<Int, Int>, Long>()
    private val disrespects = mutableMapOf<Pair<Int, Int>, Long>()


    companion object val levels = mapOf(
            1..100 to "октябрёнок",
            101..200 to "пионер",
            201..400 to "пролетарий",
            401..600 to "комсомолец",
            601..1200 to "профсоюзер",
            1201..2000 to "посол",
            2000..5000 to "генсек",
            5001..Integer.MAX_VALUE to "Гелич"
    )

    @OnCommand(["добавить", "add"], "добавить пользователю очков. /add ID COUNT", CommandPermission.ADMIN_ONLY)
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

        val count = when (message.text.split(" ").size) {
            in 1..5 -> 1
            in 6..10 -> 2
            in 11..20 -> 3
            in 21..50 -> 4
            else -> 5
        }

        addPoints(count, userId, chatId)
    }

    @OnCommand(["перевести", "отправить", "send"], "переводит e-баллы с твоей сберкнижки на другую. /перевести КОМУ КОЛИЧЕСТВО")
    fun transferPoints(messageObj: MessageNewObj) {
        val sender = messageObj.from_id
        val chatId = messageObj.peer_id
        val messageParts = regex.find(messageObj.text)
        val target = messageParts?.groupValues?.get(2)
        val count = messageParts?.groupValues?.get(3)?.let {
            Integer.parseInt(it)
        }

        if (count != null && count <= 0) {
            vk.send("Некорректное количество e-баллов", chatId)
            return
        }
        if (count == null) {
            vk.send("Неверная сумма", chatId)
            return
        }

        val targetId = target?.let {
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

        if (targetId == null) {
            vk.send("Пользователь не найден в базе Партии", chatId)
            return
        }

        if (targetId == sender) {
            vk.send("Партия рекомендует не удалять рёбра", chatId)
            return
        }

        var canSend = true
        dbQuery {
            val selected = UserScore.select{
                (UserScore.chatId eq chatId) and (UserScore.userId eq sender)
            }.firstOrNull()
            val senderPoints = selected?.get(UserScore.score) ?: 0
            if (senderPoints < count) {
                vk.send("Недостаточно средств на сберкнижке", chatId)
                canSend = false
            }
        }

        if (canSend) {
            addPoints(-count, sender, chatId)
            addPoints(count, targetId, chatId)
            vk.send("Вы перевели с Вашей сберкнижки $count e-баллов на счёт $target", chatId)
        }
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
            if (it[0] == '-') {
                "${it[0]}${it[1]}${"0".repeat(it.length - 2)}"
            } else
                "${it[0]}${"0".repeat(it.length - 1)}"
        }

        vk.send("По архивам Партии, у @$screenName уровень $levelName. Это примерно $showedScore e-баллов", chatId)
    }

    @OnCommand(["одобряю"], "показать одобрение и подкинуть чуть-чуть e-баллов. /одобряю ОДОБРЯЕМЫЙ")
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

        if (targetId == null) {
            vk.send("Партии неизвестно это лицо", peerId)
            return
        }

        if (targetId == sender) {
            vk.send("Партия рекомендует не удалять рёбра", peerId)
            return
        }

        val currentTime = System.nanoTime()
        if (respects[sender to peerId] != null && currentTime - respects[sender to peerId]!! > 1000 * 60 * 60 * 2) {
            vk.send("Партия не рекомендует одобрять другие лица чаще, чем раз в 2 часа", peerId)
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

        if (targetId == null) {
            vk.send("Партии неизвестно это лицо", peerId)
            return
        }

        if (targetId == sender) {
            vk.send("Партия рекомендует не удалять рёбра", peerId)
            return
        }

        val currentTime = System.nanoTime()
        if (
                disrespects[sender to peerId] != null &&
                currentTime - disrespects[sender to peerId]!! > 1000 * 60 * 60 * 2
        ) {
            vk.send("Партия не рекомендует осуждать другие лица чаще, чем раз в 2 часа", peerId)
            return
        }

        disrespects[sender to peerId] = currentTime
        addPoints(-10, targetId, peerId)
        vk.send("Осуждение выражено", peerId)
    }


    private fun addPoints(count: Int, toUser: Int, chat: Int) {
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
            vk.send("Партия поздравляет @$screenName с повышением до $name!", chat)
        }
    }

    private fun getLevelName(score: Int): String {
        for ((key, value) in levels) {
            if (score in key) return value
        }
        return "ЗАСЕКРЕЧЕНО"
    }
}