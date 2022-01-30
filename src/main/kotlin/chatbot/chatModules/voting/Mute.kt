package chatbot.chatModules.voting

import api.*
import api.GarbageMessage.Companion.toGarbageMessageWithDelay
import api.GarbageMessagesCollector.Companion.DEFAULT_DELAY
import api.GarbageMessagesCollector.Companion.MINUTE_DELAY
import chatbot.CommandPermission
import chatbot.ModuleObject
import chatbot.OnCommand
import chatbot.chatBotEvents.LongPollNewMessageEvent
import java.util.concurrent.ConcurrentHashMap

@ModuleObject
object Mute : Votable() {
    private const val muteMinuteTime = 12 * 60
    val mutedTime = ConcurrentHashMap<Pair<Int, Int>, Long>()

    @OnCommand(["mute", "мут", "заглушить", "shutup"], "голосование на выдачу бесплатного мьюта")
    fun votingMute(event: LongPollNewMessageEvent) {
        voting(event) { api, chatId, _, target ->
            val screenName = target.targetScreenName
            if (target.targetId to chatId in mutedTime.keys) {
                api.send("Партия уже заглушила $screenName", chatId)
                return@voting null
            }
            val senderScreenName = api.getUserNameById(event.userId)
            val votingForMessage = "Голосование на лишение голоса $screenName началось\n" +
                    "Отправь /mute ${target.rawText}, чтобы проголосовать за, или /stopmute ${target.rawText}, если ты хочешь слышать товарища!"
            val successVoteMessage = "$senderScreenName не хочет слышать $screenName"
            val keyboardPositiveMessage = "/mute ${target.rawText}"
            val keyboardNegativeMessage = "/stopmute ${target.rawText}"
            val onEndVotingMessage =
                "Вы лишены голоса на $muteMinuteTime минут, $screenName, переосмыслите хорошенько свои взгляды!!"
            val onTimeIsUp = "Голосование окончено, $screenName будет и дальше нас радовать своими прекрасными речами!"
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

    @OnCommand(
        ["stopmute", "пустьговорит", "голос"],
        "он все равно будет говорить в наших сердцах...",
        showInHelp = false
    )
    fun justifyMute(event: LongPollNewMessageEvent) {
        votingAgainst(event) { api, chatId, _, target ->
            val screenName = target.targetScreenName
            if (super.voting[target.targetId to chatId] == null) {
                api.send("Нет подходящего голосования $screenName", chatId)
                return@votingAgainst null
            }
            val senderScreenName = api.getUserNameById(event.userId)
            val successVoteMessage = "$senderScreenName хочет слышать прекрасный голос $screenName"
            Messages(successVoteMessage = successVoteMessage)
        }
    }

    @OnCommand(["unmute"], "вернуть человеку голос", CommandPermission.ADMIN)
    fun cancelVotingResultGulag(event: LongPollNewMessageEvent) {
        cancelVotingResult(event) { api, chatId, _, target ->
            if (mutedTime.remove(target.targetId to chatId) == null) {
                api.send("Данного человека нет в архивах 'Лишенных голоса'", chatId)
                return@cancelVotingResult
            }

            api.send("Заговор на снятие немоты прошел успешно, ${target.targetScreenName} говори!", chatId)
        }
    }

    @OnCommand(["admmute"], "тебя никто не услышит...", CommandPermission.ADMIN, showInHelp = false)
    fun adminVotingGulag(event: LongPollNewMessageEvent) {
        adminVoting(event) { _, _, _, target ->
            val onEndVotingMessage = "В чате появился немой человек, ${target.targetScreenName}, что случилось!?"
            Messages(onEndVotingMessage = onEndVotingMessage)
        }
    }

    override fun onEndVoting(targetId: Int, chatId: Int, api: VkApi) {
        val currentTime = System.currentTimeMillis()
        mutedTime[targetId to chatId] = currentTime + 1000 * 60 * muteMinuteTime
        voting.remove(targetId to chatId)
    }

    @OnCommand(["mutelist", "muted"], " показать список самых вежливых людей", showInHelp = false)
    fun showMuteList(event: LongPollNewMessageEvent) {
        GarbageMessagesCollector.addGarbageMessage(event.toGarbageMessageWithDelay(DEFAULT_DELAY))
        val mutedInCurrentChat = mutedTime.keys.filter { it.second == event.chatId }
        if (mutedInCurrentChat.isNotEmpty()) {
            val answer = buildString {
                append("Список вежливых людей:\n")
                val currentTime = System.currentTimeMillis()
                for (muted in mutedInCurrentChat) {
                    val stillMutedMinutes = (mutedTime[muted]!! - currentTime) / (1000 * 60)
                    val targetId = muted.first
                    append("@id$targetId будет вежливым еще целых $stillMutedMinutes минут!\n")
                }
            }
            event.api.send(answer, event.chatId, removeDelay = MINUTE_DELAY)
        } else {
            event.api.send("Список вежливых людей пуст", event.chatId, removeDelay = DEFAULT_DELAY)
        }
    }

    override var targetNoneMessage: String = "Укажите кого заглушить"
    override var targetNullMessage: String = "Тот, кого нет, не может говорить"
    override var targetNoneGetBackMessage: String = "Укажите заглушенного"
    override var targetEqualsSenderMessage: String = "Замолчите добровольно, пока вас не замолчали"
    override var targetDefendHimSelf: String =
        "Расскажите людям почему этого делать не стоит, пока вы еще можете говорить..."
    override var alreadyChoseSide: String = ", вы уже высказали свое мнение"
    override var targetEqualsBotMessage: String = "АХАХАХАХАХ, нет..."
    override var alreadyVotedMessage = ", ваш голос услышан"
}