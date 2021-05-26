package chatbot.chatModules

import api.*
import chatbot.ModuleObject
import chatbot.OnCommand
import chatbot.chatBotEvents.LongPollNewMessageEvent
import database.UserReward
import database.dbQuery
import org.jetbrains.exposed.sql.insert
import java.lang.StringBuilder

@ModuleObject
object Reward : Votable() {
    private var rewardNameStr = ""

    @OnCommand(["наградить", "reward"], "голосование за награждение товарища")
    fun votingReward(event: LongPollNewMessageEvent) {
        voting(event) { api, chatId, _, target ->
            val targetId = target.targetId
            val isNewVoting =
                voting[targetId to chatId] == null || voting[targetId to chatId]!!.timeOfClosing < event.time
            val rewardNameInMessage = TextMessageParser().parse(event.text).getRewardName()
            rewardNameStr = if (rewardNameInMessage != "") rewardNameInMessage else rewardNameStr
            if (rewardNameStr == "" && isNewVoting) {
                api.send("Пожалуйста, укажите название награды в квадратных скобках после имени награждаемого", chatId)
                return@voting null
            }
            val screenName = target.targetScreenName

            val votingForMessage = "Голосование за вручение $screenName награды $rewardNameStr\n" +
                    "Отправь /наградить ${target.rawText}"

            val successVoteMessage = "за награждение $screenName"

            val keyboardMessage = "/наградить ${target.rawText}"

            val onEndVotingMessage = "$screenName получает награду $rewardNameStr"
            Messages(votingForMessage, successVoteMessage, keyboardMessage, onEndVotingMessage)
        }
    }

    override fun onEndVoting(targetId: Int, chatId: Int, api: VkApi) {
        dbQuery {
            UserReward.insert {
                it[this.chatId] = chatId
                it[userId] = targetId
                it[rewardName] = rewardNameStr
            }
        }
        rewardNameStr = ""
    }

    override var targetNoneMessage: String = "Не указан награждаемый"
    override var targetNoneGetBackMessage: String = ""
    override var targetNullMessage: String = "Товарищ, нельзя наградить того, кого нет"
    override var targetEqualsSenderMessage: String = "Товарищ! Вы не Брежнев"
    override var targetEqualsBotMessage: String = "Партии не нужны награды, партии нужен только коммунизм!"
    override var alreadyVotedMessage = ", Вы уже проголосовали за награждение этого товарища"

    private fun ParseObject.getRewardName(): String {
        for (i in 2 until this.size) {
            val tokenText = this.get<Text>(i)?.rawText ?: continue
            if (tokenText.startsWith("[") && tokenText.endsWith("]")) {
                return tokenText.substring(1 until tokenText.length - 1) // removing []
            }
        }

        return ""
    }
}