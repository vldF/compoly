package modules.chatbot.chatModules

import api.Mention
import api.TextMessageParser
import api.VkPlatform
import modules.Active
import modules.chatbot.CommandPermission
import modules.chatbot.OnCommand
import modules.chatbot.chatBotEvents.LongPollNewMessageEvent
import java.lang.Thread.sleep
import java.util.concurrent.ConcurrentHashMap

@Active
class Gulag {
    private val gulagVoting = mutableMapOf<Pair<Long, Long>, Voting>()
    private val gulagTimeout = mutableMapOf<Pair<Long, Long>, Long>()

    private val coefficientForKick = 0.3 // Процент от онлайна, нужный для кика
    private val minCount = 10 // Минимальное кол-во людей для кика
    private val kickMinuteTime = 12 * 60 // Время нахождения в ГУЛАГе

    companion object {
        val gulagKickTime = ConcurrentHashMap<Pair<Long, Long>, Long>()
    }

    @OnCommand(["гулаг", "gulag"], "голосование на отправление в трудовой лагерь")
    fun gulag(event: LongPollNewMessageEvent) {
        val api = event.api
        val chatId = event.chatId
        val sender = event.userId

        val parsed = TextMessageParser(event.platform).parse(event.text)
        val target = parsed.get<Mention>(1)
        val targetId = target?.targetId
        if (target == null) {
            api.send("Не указан ссыльный", chatId)
            return
        }

        if (targetId == null) {
            api.send("Товарищ, нельзя сослать того, кого нет", chatId)
            return
        }

        if (targetId == sender) {
            api.send("Товарищ! Вы еще нужны своей родине", chatId)
            return
        }

        val screenName = target.targetScreenName
        if (targetId to chatId in gulagKickTime.keys) {
            api.send("Партия уже наказала $screenName", chatId)
            return
        }

        val currentTime = System.currentTimeMillis()
        if (
            gulagTimeout[sender to chatId] != null &&
            currentTime - gulagTimeout[sender to chatId]!! < 1000 * 60 * 60 * 4
        ) {
            api.send("Партия не рекомендует отправлять в ГУЛАГ других лиц чаще, чем раз в 4 часа", chatId)
            return
        }

        if (gulagVoting[targetId to chatId] == null ||
            gulagVoting[targetId to chatId]!!.timeOfClosing < currentTime) {
            val onlineCount = 10 // todo: get this value via API
            val count = (onlineCount * coefficientForKick).toInt()
            val newVoting = Voting(currentTime + 1000 * 60 * 5, if (count > minCount) count else minCount)

            newVoting.addVote(sender, chatId)
            gulagVoting[targetId to chatId] = newVoting
            api.send(
               "Голосование на отправление $screenName в лагерь началось - 1/${newVoting.rightNumToVote}\n" +
                    "Отправь /гулаг $screenName",
                    chatId
            )
        } else {
            val votingIsComplete = gulagVoting[targetId to chatId]!!.addVote(sender, chatId)
            api.send(
                "Отправление $screenName в лагерь - ${gulagVoting[targetId to chatId]!!.getVotes()}/${gulagVoting[targetId to chatId]!!.rightNumToVote}",
                chatId
            )
            if (votingIsComplete) {
                api.send("Подумай над своим поведением, $screenName, а потом напиши админам, чтобы тебя позвали назад", chatId)
                sleep(500)
                api.kickUserFromChat(targetId, chatId)
                gulagKickTime[targetId to chatId] = currentTime + 1000 * 60 * kickMinuteTime
                gulagVoting.remove(targetId to chatId)
            }
        }
        gulagTimeout[sender to chatId] = currentTime
    }

    @OnCommand(["вернуть", "back"], "вернуть из ссылки", CommandPermission.ADMIN_ONLY)
    fun back(event: LongPollNewMessageEvent) {
        val api = event.api
        val chatId = event.chatId
        val sender = event.userId

        val parsed = TextMessageParser(event.platform).parse(event.text)
        val target = parsed.get<Mention>(1)
        val targetId = target?.targetId
        if (target == null) {
            api.send("Не указан ссыльный", chatId)
            return
        }

        if (targetId == null) {
            api.send("Товарищ, нельзя сослать того, кого нет", chatId)
            return
        }

        if (targetId == sender) {
            api.send("Товарищ! Вы еще нужны своей родине", chatId)
            return
        }

        if (gulagKickTime.remove(targetId to chatId) == null) {
            api.send("Данного человека нет в архивах ГУЛАГ", chatId)
            return
        }

        api.send("$target может вернуться досрочно", chatId)
    }

    @OnCommand(["admgulag"], "В гулаг без суда и следствия", CommandPermission.ADMIN_ONLY)
    fun admgulag(event: LongPollNewMessageEvent) {
        val api = event.api
        val chatId = event.chatId
        val sender = event.userId

        val parsed = TextMessageParser(event.platform).parse(event.text)
        val target = parsed.get<Mention>(1)
        val targetId = target?.targetId
        if (target == null) {
            api.send("Не указан ссыльный", chatId)
            return
        }

        if (targetId == null) {
            api.send("Товарищ, нельзя сослать того, кого нет", chatId)
            return
        }

        if (targetId == sender) {
            api.send("Товарищ! Вы еще нужны своей родине", chatId)
            return
        }

        api.send("Подумай над своим поведением, ${target.targetScreenName}, а потом напиши админам, чтобы тебя позвали назад", chatId)
        sleep(500)
        api.kickUserFromChat(chatId, targetId)
        val currentTime = System.currentTimeMillis()
        gulagKickTime[targetId to chatId] = currentTime + 1000 * 60 * kickMinuteTime
        gulagVoting.remove(targetId to chatId)
        gulagTimeout[sender to chatId] = currentTime

    }
}