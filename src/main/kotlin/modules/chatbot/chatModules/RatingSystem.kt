package modules.chatbot.chatModules

import api.UserScore
import api.Vk
import api.password
import api.userName
import log
import modules.Active
import modules.chatbot.MessageNewObj
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

@Active
class RatingSystem : Command {
    override val keyWord: List<String> = listOf("/add")
    override val permission: CommandPermission = CommandPermission.ADMIN_ONLY
    override val description: String = "Добавить пользователю очков. /add ID COUNT"

    private val regex = Regex("^(.+)\\s(.+)\\s(\\d+)\$")
    private val mentionRegex = Regex("\\[id(\\d+)(.+)")

    init {
        Database.connect(
            "jdbc:postgresql://130.61.203.95/compoly",
            driver = "org.postgresql.Driver",
            user = userName,
            password = password)
    }

    override fun call(messageObj: MessageNewObj) {
        val peerId = messageObj.peer_id
        val messageParts = regex.find(messageObj.text)
        val target = messageParts?.groupValues?.get(2)
        val deltaScore = messageParts?.groupValues?.get(3)?.let {
            Integer.parseInt(it)
        }

        val targetId = target?.let {
            if (it.contains("[id"))
                mentionRegex.find(it)?.groupValues?.get(1).let { v -> Integer.parseInt(v) }
            else {
                val name = when {
                    it.contains("vk.com/") -> it.split("vk.com/")[1]
                    it.startsWith("@") -> it.removePrefix("@")
                    else -> it
                }
                Vk().getUserId(name)
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
                Vk().send("Неверные аргументы, товарищ", peerId)
                return
        }

        transaction {
            val selected = UserScore.select{
                (UserScore.chatId eq peerId) and (UserScore.userId eq targetId)
            }.firstOrNull()
            if (selected == null) {
                UserScore.insert {
                    it[chatId] = peerId
                    it[userId] = targetId
                    it[score] = deltaScore
                }
            } else {
                UserScore.update({ (UserScore.chatId eq peerId) and (UserScore.userId eq targetId) }) {
                    it[score] = selected[score] +  deltaScore
                }
            }
            commit()
        }
    }

}