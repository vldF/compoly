package modules.chatbot.chatModules

import api.Mention
import api.TelegramPlatform
import api.TextMessageParser
import api.keyboards.KeyboardBuilder
import api.keyboards.KeyboardButton
import api.keyboards.KeyboardColor
import modules.chatbot.CommandPermission
import modules.chatbot.ModuleObject
import modules.chatbot.OnCommand
import modules.chatbot.chatBotEvents.LongPollNewMessageEvent
import java.lang.Thread.sleep
import java.util.concurrent.ConcurrentHashMap

@ModuleObject
object Gulag {
    private val gulagVoting = mutableMapOf<Pair<Long, Long>, Voting>()
    private val votedIds = mutableMapOf<Long, MutableSet<Long>>()

    private const val coefficientForKick = 0.3 // Процент от онлайна, нужный для кика
    private const val minCount = 10 // Минимальное кол-во людей для кика
    private const val kickMinuteTime = 12 * 60 // Время нахождения в ГУЛАГе
    val gulagKickTime = ConcurrentHashMap<Pair<Long, Long>, Long>()

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

        if (targetId == api.meId) {
            api.send("Пара воронков уже выехали", chatId)
            return
        }

        val screenName = target.targetScreenName
        if (targetId to chatId in gulagKickTime.keys) {
            api.send("Партия уже наказала $screenName", chatId)
            return
        }

        if (votedIds[targetId]?.contains(sender) == true) {
            val senderScreenName = api.getUserNameById(sender)
            api.send("$senderScreenName, вы уже проголосовали за этого предателя родины", chatId)
            return
        }

        val currentTime = System.currentTimeMillis()
        if (gulagVoting[targetId to chatId] == null ||
            gulagVoting[targetId to chatId]!!.timeOfClosing < currentTime) {
            val onlineCount = 10 // todo: get this value via API
            val count = (onlineCount * coefficientForKick).toInt()
            val newVoting = Voting(currentTime + 1000 * 60 * 5, if (count > minCount) count else minCount)

            newVoting.addVote(sender, chatId)
            gulagVoting[targetId to chatId] = newVoting

            val keyboard = KeyboardBuilder()
                    .addButton(KeyboardButton("/гулаг ${target.rawText}", "за", KeyboardColor.NEGATIVE))
                    .build()

            api.send(
                    "Голосование на отправление $screenName в лагерь началось - 1/${newVoting.rightNumToVote}\n" +
                            "Отправь /гулаг ${target.rawText}",
                    chatId,
                    keyboard = keyboard
            )

        } else {
            val votingIsComplete = gulagVoting[targetId to chatId]!!.addVote(sender, chatId)
            val senderScreenName = api.getUserNameById(sender)
            api.send(
                "$senderScreenName проголосовал за отправление $screenName в лагерь [${gulagVoting[targetId to chatId]!!.getVotes()}/${gulagVoting[targetId to chatId]!!.rightNumToVote}]",
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

        if (votedIds[targetId] == null) {
            votedIds[targetId] = mutableSetOf(sender)
        } else {
            votedIds[targetId]!!.add(sender)
        }
    }

    @OnCommand(["вернуть", "back"], "вернуть из ссылки", CommandPermission.ADMIN)
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

    @OnCommand(["admgulag"], "В гулаг без суда и следствия", CommandPermission.ADMIN)
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
    }
}