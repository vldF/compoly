package modules.chatbot.chatModules

import api.VkPlatform
import modules.Active
import modules.chatbot.CommandPermission
import modules.chatbot.MessageNewObj
import modules.chatbot.OnCommand
import java.lang.IllegalStateException
import java.lang.Thread.sleep
import java.util.concurrent.ConcurrentHashMap

@Active
class Gulag {
    private val mentionRegex = Regex("id(\\d+)(.*)")
    private val gulagVoting = mutableMapOf<Pair<Int, Int>, Voting>()
    private val gulagTimeout = mutableMapOf<Pair<Int, Int>, Long>()

    private val koefForKick = 0.3 // Процент от онлайна, нужный для кика
    private val minCount = 10 // Минимальное кол-во людей для кика
    private val kickMinuteTime = 12 * 60 // Время нахождения в ГУЛАГе

    companion object {
        private val vk = VkPlatform()
        val gulagKickTime = ConcurrentHashMap<Pair<Int, Int>, Long>()
    }

    @OnCommand(["гулаг", "gulag"], "голосование на отправление в трудовой лагерь")
    fun gulag(messageObj: MessageNewObj) {
        val peerId = messageObj.peer_id
        val sender = messageObj.from_id
        val parts = messageObj.text.split(" ")
        if (parts.size < 2) {
            vk.send("Не указан ссыльный", peerId)
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
                vk.getUserIdByName(name)
            }
        }

        if (targetId == null) {
            vk.send("Товарищ, нельзя сослать того, кого нет", peerId)
            return
        }

        if (targetId == sender) {
            vk.send("Товарищ! Вы еще нужны своей родине", peerId)
            return
        }

        val screenName = vk.getUserNameById(targetId)
        if (targetId to peerId in gulagKickTime.keys) {
            vk.send("Партия уже наказала $screenName", peerId)
            return
        }

        val currentTime = System.currentTimeMillis()
        if (
            gulagTimeout[sender to peerId] != null &&
            currentTime - gulagTimeout[sender to peerId]!! < 1000 * 60 * 60 * 4
        ) {
            vk.send("Партия не рекомендует отправлять в ГУЛАГ других лиц чаще, чем раз в 4 часа", peerId)
            return
        }

        if (gulagVoting[targetId to peerId] == null ||
            gulagVoting[targetId to peerId]!!.timeOfClosing < currentTime) {
            val members = vk.getChatMembers(messageObj.peer_id, listOf("online"))
            val onlineCount = members?.filter { it.online!! == 1}?.size
                    ?: throw IllegalStateException("error on getting members")
            val count = (onlineCount * koefForKick).toInt()
            val newVoting = Voting(currentTime + 1000 * 60 * 5, if (count > minCount) count else minCount)

            newVoting.addVote(sender, peerId)
            gulagVoting[targetId to peerId] = newVoting
            vk.send(
               "Голосование на отправление $screenName в лагерь началось - 1/${newVoting.rightNumToVote}\n" +
                    "Отправь /гулаг $screenName",
                    peerId
            )
        } else {
            val votingIsComplete = gulagVoting[targetId to peerId]!!.addVote(sender, peerId)
            vk.send(
                "Отправление $screenName в лагерь - ${gulagVoting[targetId to peerId]!!.getVotes()}/${gulagVoting[targetId to peerId]!!.rightNumToVote}",
                peerId
            )
            if (votingIsComplete) {
                vk.send("Подумай над своим поведением, $screenName, а потом напиши админам, чтобы тебя позвали назад", peerId)
                sleep(500)
                vk.kickUserFromChat(targetId, peerId)
                gulagKickTime[targetId to peerId] = currentTime + 1000 * 60 * kickMinuteTime
                gulagVoting.remove(targetId to peerId)
            }
        }
        gulagTimeout[sender to peerId] = currentTime
    }

    @OnCommand(["вернуть", "back"], "вернуть из ссылки", CommandPermission.ADMIN_ONLY)
    fun back(messageObj: MessageNewObj) {
        val peerId = messageObj.peer_id
        val sender = messageObj.from_id
        val parts = messageObj.text.split(" ")
        if (parts.size < 2) {
            vk.send("Не указан ссыльный", peerId)
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
                vk.getUserIdByName(name)
            }
        }

        if (targetId == null) {
            vk.send("Товарищ, нельзя вернуть того, кого нет", peerId)
            return
        }

        if (targetId == sender) {
            vk.send("Товарищ! Вы еще нужны своей родине", peerId)
            return
        }

        if (gulagKickTime.remove(targetId to peerId) == null) {
            vk.send("Данного человека нет в архивах ГУЛАГ", peerId)
            return
        }

        vk.send("$target может вернуться досрочно", peerId)
    }

    @OnCommand(["admgulag"], "В гулаг без суда и следствия", CommandPermission.ADMIN_ONLY)
    fun admgulag(messageObj: MessageNewObj) {
        val peerId = messageObj.peer_id
        val sender = messageObj.from_id
        val parts = messageObj.text.split(" ")
        if (parts.size < 2) {
            vk.send("Не указан ссыльный", peerId)
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
                vk.getUserIdByName(name)
            }
        }

        if (targetId == null) {
            vk.send("Товарищ, нельзя сослать того, кого нет", peerId)
            return
        }

        if (targetId == sender) {
            vk.send("Товарищ! Вы еще нужны своей родине", peerId)
            return
        }

        vk.send("Подумай над своим поведением, $target, а потом напиши админам, чтобы тебя позвали назад", peerId)
        sleep(500)
        vk.kickUserFromChat(targetId, peerId)
        val currentTime = System.currentTimeMillis()
        gulagKickTime[targetId to peerId] = currentTime + 1000 * 60 * kickMinuteTime
        gulagVoting.remove(targetId to peerId)
        gulagTimeout[sender to peerId] = currentTime

    }
}