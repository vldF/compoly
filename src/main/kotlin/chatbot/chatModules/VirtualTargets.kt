package chatbot.chatModules

import api.Mention
import api.TextMessageParser
import chatbot.CommandPermission
import chatbot.ModuleObject
import chatbot.OnCommand
import chatbot.chatBotEvents.LongPollNewMessageEvent
import database.UserScore
import database.VirtualMentions
import database.dbQuery
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select

@ModuleObject
object VirtualTargets {
    @OnCommand(["addvirtualtarget"], "создать виртуальную цель для одобрения и осуждения", CommandPermission.ADMIN)
    fun create(event: LongPollNewMessageEvent) {
        val api = event.api
        val chatId = event.chatId
        val parser = TextMessageParser().parse(event.text, chatId)
        val name = parser.get<Mention>(1)?.targetScreenName?.toLowerCase()
        if (name == null) {
            api.send("Укажите имя цели", chatId)
            return
        }

        if (parser.size > 2) {
            api.send("Указано больше одной цели", chatId)
            return
        }

        if (isTargetExists(name, chatId)) {
            api.send("Эта цель уже известна Партии: $name", chatId)
            return
        }

        val id = addTargetAndGetId(name, chatId)
        if (id == null) {
            api.send("Ошибка при создании цели", chatId)
            return
        }

        RatingSystem.addReputation(0, id, chatId, api)
        api.send("Цель успешно добавлена в архивы: $name", chatId)
    }

    @OnCommand(["deletevirtualtarget"], "удалить виртуальную цель для одобрения и осуждения", CommandPermission.ADMIN)
    fun delete(event: LongPollNewMessageEvent) {
        val api = event.api
        val chatId = event.chatId
        val parser = TextMessageParser().parse(event.text, chatId)
        val name = parser.get<Mention>(1)?.targetScreenName?.toLowerCase()
        if (name == null) {
            api.send("Укажите имя цели", chatId)
            return
        }
        if (!isTargetExists(name, chatId)) {
            api.send("Партия не знает эту цель: $name", chatId)
            return
        }

        deleteTarget(name, chatId)
        api.send("Цель удалена успешно", chatId)
    }

    @OnCommand(["виртуальные", "виртуальныедосье", "virtuallist"], "показать все виртуальные цели")
    fun virtualTargetsList(event: LongPollNewMessageEvent) {
        val api = event.api
        val chatId = event.chatId

        val namesAndLevels = dbQuery {
            val targets = VirtualMentions.select {
                VirtualMentions.chatId eq chatId
            }.associateBy { it[VirtualMentions.id] }
            val targetIds = targets.keys

            val scores = UserScore.select {
                UserScore.userId inList targetIds
            }.associate { it[UserScore.userId] to it[UserScore.reputation] }.toMutableMap()

            for (id in targetIds) {
                if (id !in scores.keys) {
                    scores[id] = -1
                }
            }

            scores.map { (id, score) ->
                (targets[id]?.get(VirtualMentions.name)!!) to RatingSystem.Level.getLevel(score).levelName
            }
        }

        if (namesAndLevels.isEmpty()) {
            api.send("Виртуальные досье не найдены", chatId)
            return
        }

        val answer = namesAndLevels.joinToString(separator = "\n", prefix = "Виртуальные досье:\n") { "${it.first}: ${it.second}" }
        api.send(answer, chatId)
    }

    fun getVirtualNameById(id: Int): String? {
        return dbQuery {
            VirtualMentions.select {
                VirtualMentions.id eq id
            }.firstOrNull()?.get(VirtualMentions.name)
        }
    }

    private fun isTargetExists(name: String, chatId: Int): Boolean {
        return dbQuery {
            VirtualMentions.select {
                (VirtualMentions.chatId eq chatId) and (VirtualMentions.name eq name)
            }.firstOrNull() != null
        }
    }

    private fun addTargetAndGetId(name: String, chatId: Int): Int? {
        return dbQuery {
            VirtualMentions.insert {
                it[this.name] = name
                it[this.chatId] = chatId
            }

            VirtualMentions.select {
                (VirtualMentions.chatId eq chatId) and (VirtualMentions.name eq name)
            }.firstOrNull()?.get(VirtualMentions.id)
        }
    }

    private fun deleteTarget(name: String, chatId: Int) {
        dbQuery {
            VirtualMentions.deleteWhere {
                (VirtualMentions.chatId eq chatId) and (VirtualMentions.name eq name)
            }
        }
    }
}