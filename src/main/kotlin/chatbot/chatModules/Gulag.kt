package chatbot.chatModules

import api.*
import chatbot.CommandPermission
import chatbot.ModuleObject
import chatbot.OnCommand
import chatbot.chatBotEvents.LongPollNewMessageEvent
import java.lang.Thread.sleep
import java.util.concurrent.ConcurrentHashMap

@ModuleObject
object Gulag : Votable() {
    private const val kickMinuteTime = 12 * 60 // Время нахождения в ГУЛАГе
    val gulagKickTime = ConcurrentHashMap<Pair<Int, Int>, Long>()

    @OnCommand(["гулаг", "gulag"], "голосование на отправление в трудовой лагерь")
    override fun voting(event: LongPollNewMessageEvent) {
        val api = event.api
        val parsed = TextMessageParser().parse(event.text)
        val target = parsed.get<Mention>(1)
        if (target != null) {
            val screenName = target.targetScreenName
            if (target.targetId to event.chatId in gulagKickTime.keys) {
                api.send("Партия уже наказала $screenName", event.chatId)
                return
            }
            votingForMessage = "Голосование на отправление $screenName в лагерь началось\n" +
                    "Отправь /гулаг ${target.rawText}"
            successVoteMessage = "за отправление $screenName в лагерь"
            keyboardMessage = "/гулаг ${target.rawText}"
            onEndVotingMessage =
                "Подумай над своим поведением, $screenName, а потом напиши админам, чтобы тебя позвали назад"
        }
        super.voting(event)
    }

    @OnCommand(["вернуть", "back"], "вернуть из ссылки", CommandPermission.ADMIN)
    override fun cancelVotingResult(event: LongPollNewMessageEvent) {
        super.cancelVotingResult(event)
        val api = event.api
        val chatId = event.chatId

        val parsed = TextMessageParser().parse(event.text)
        val target = parsed.get<Mention>(1)
        val targetId = target?.targetId

        if (gulagKickTime.remove(targetId to chatId) == null) {
            api.send("Данного человека нет в архивах ГУЛАГ", chatId)
            return
        }

        api.send("${target!!.targetScreenName} может вернуться досрочно", chatId)
    }

    @OnCommand(["admgulag"], "В гулаг без суда и следствия", CommandPermission.ADMIN, showOnHelp = false)
    override fun adminVoting(event: LongPollNewMessageEvent) {
        super.adminVoting(event)
        val api = event.api
        val chatId = event.chatId

        val parsed = TextMessageParser().parse(event.text)
        val target = parsed.get<Mention>(1)
        val targetId = target?.targetId

        api.send(
            "Подумай над своим поведением, ${target!!.targetScreenName}, а потом напиши админам, чтобы тебя позвали назад",
            chatId
        )
        sleep(500)
        api.kickUserFromChat(chatId, targetId!!)
        val currentTime = System.currentTimeMillis()
        gulagKickTime[targetId to chatId] = currentTime + 1000 * 60 * kickMinuteTime
        voting.remove(targetId to chatId)
    }

    override fun onEndVoting(targetId: Int, chatId: Int, api: VkPlatform) {
        sleep(500)
        api.kickUserFromChat(chatId, targetId)
        val currentTime = System.currentTimeMillis()
        gulagKickTime[targetId to chatId] = currentTime + 1000 * 60 * kickMinuteTime
        voting.remove(targetId to chatId)
    }

    override var targetNoneMessage: String = "Не указан ссыльный"
    override var targetNullMessage: String = "Товарищ, нельзя сослать того, кого нет"
    override var targetNoneGetBackMessage: String = "Не указан сосланный"
    override var targetEqualsSenderMessage: String = "Товарищ! Вы еще нужны своей Родине"
    override var targetEqualsBotMessage: String = "Пара воронков уже выехали"
    override var alreadyVotedMessage = ", Вы уже проголосовали за этого предателя Родины"
}