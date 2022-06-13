package chatbot.chatModules.voting

import api.*
import chatbot.CommandPermission
import chatbot.ModuleObject
import chatbot.OnCommand
import chatbot.chatBotEvents.LongPollNewMessageEvent
import java.lang.Thread.sleep
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@ModuleObject
object Gulag : Votable() {
    private val kickDuration = Duration.ofHours(12) // Время нахождения в ГУЛАГе
    val gulagKickTime = ConcurrentHashMap<Pair<Int, Int>, Long>()

    @OnCommand(["гулаг", "gulag"], "голосование на отправление в трудовой лагерь")
    fun votingGulag(event: LongPollNewMessageEvent) {
        voting(event) { api, chatId, _, target ->
            val screenName = target.targetScreenName
            if (target.targetId to chatId in gulagKickTime.keys) {
                api.send("Партия уже наказала $screenName", chatId)
                return@voting null
            }
            val senderScreenName = api.getUserNameById(event.userId)
            val votingForMessage = "Голосование на отправление $screenName в лагерь началось\n" +
                    "Отправь /гулаг ${target.rawText}, чтобы проголосовать за, или /оправдать ${target.rawText}, если ты ручаешься за товарища!"
            val successVoteMessage = "$senderScreenName проголосовал за отправление $screenName в лагерь"
            val keyboardPositiveMessage = "/гулаг ${target.rawText}"
            val keyboardNegativeMessage = "/оправдать ${target.rawText}"
            val onEndVotingMessage =
                "Подумай над своим поведением, $screenName, а потом напиши админам, чтобы тебя позвали назад"
            val onTimeIsUp = "Голосование окончено, сегодня партия милует $screenName!"
            Messages(
                votingForMessage,
                successVoteMessage,
                keyboardPositiveMessage,
                keyboardNegativeMessage,
                onEndVotingMessage,
                onTimeIsUp
            )
        }
    }

    @OnCommand(["оправдать", "justify"], "свободу политосужденным!", showInHelp = false)
    fun justifyMember(event: LongPollNewMessageEvent) {
        votingAgainst(event) { api, chatId, _, target ->
            val screenName = target.targetScreenName
            if (super.voting[target.targetId to chatId] == null) {
                api.send("Партия не ссудит $screenName", chatId)
                return@votingAgainst null
            }
            val senderScreenName = api.getUserNameById(event.userId)
            val successVoteMessage = "$senderScreenName проголосовал против отправления $screenName в лагерь"
            Messages(successVoteMessage = successVoteMessage)
        }
    }

    @OnCommand(["вернуть", "back"], "вернуть из ссылки", CommandPermission.ADMIN)
    fun cancelVotingResultGulag(event: LongPollNewMessageEvent) {
        cancelVotingResult(event) { api, chatId, _, target ->
            if (gulagKickTime.remove(target.targetId to chatId) == null) {
                api.send("Данного человека нет в архивах ГУЛАГ", chatId)
                return@cancelVotingResult
            }

            api.send("${target.targetScreenName} может вернуться досрочно", chatId)
        }
    }

    @OnCommand(["admgulag"], "В гулаг без суда и следствия", CommandPermission.ADMIN, showInHelp = false)
    fun adminVotingGulag(event: LongPollNewMessageEvent) {
        adminVoting(event) { _, _, _, target ->
            val onEndVotingMessage =
                "Подумай над своим поведением, ${target.targetScreenName}, а потом напиши админам, чтобы тебя позвали назад"
            Messages(onEndVotingMessage = onEndVotingMessage)
        }
    }

    override fun onEndVoting(targetMention: Mention, chatId: Int, api: VkApi) {
        sleep(500)
        val targetId = targetMention.targetId ?: error("target id is null for mention $targetMention")
        api.kickUserFromChat(chatId, targetId)
        val endTime = System.currentTimeMillis() + kickDuration.toMillis()
        gulagKickTime[targetId to chatId] = endTime
        voting.remove(targetId to chatId)
        sendDelayedMessage(
            message = "Пользователь ${targetMention.targetScreenName} может вернуться в чат",
            chatId = chatId,
            sendTimeMillis = AtomicLong(endTime),
        ) {
            gulagKickTime[targetId to chatId] != null
        }
    }

    override var targetNoneMessage: String = "Не указан ссыльный"
    override var targetNullMessage: String = "Товарищ, нельзя сослать того, кого нет"
    override var targetNoneGetBackMessage: String = "Не указан сосланный"
    override var targetEqualsSenderMessage: String = "Товарищ! Вы еще нужны своей Родине"
    override var targetDefendHimSelf: String = "У вас нет права голоса..."
    override var alreadyChoseSide: String = ", Вы уже высказали свое мнение"
    override var targetEqualsBotMessage: String = "Пара воронков уже выехали"
    override var alreadyVotedMessage = ", Вы уже проголосовали за этого предателя Родины"
}