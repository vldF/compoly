package chatbot.chatModules.voting

import api.*
import chatbot.ModuleObject
import chatbot.OnCommand
import chatbot.chatBotEvents.LongPollNewMessageEvent
import database.UserReward
import database.dbQuery
import org.jetbrains.exposed.sql.insert

@ModuleObject
object Reward : Votable() {
    private var rewardsMap = mutableMapOf<String, String?>()

    @OnCommand(["наградить", "reward"], "голосование за награждение товарища")
    fun votingReward(event: LongPollNewMessageEvent) {
        voting(event) { api, chatId, _, target ->
            val targetId = target.targetId
            val isNewVoting =
                voting[targetId to chatId] == null || voting[targetId to chatId]!!.isTimeUp
            val rewardNameInMessage = TextMessageParser().parse(event.text).getRewardName()

            val key = "$chatId.$targetId"

            if (isNewVoting && rewardNameInMessage != "") {
                rewardsMap[key] = rewardNameInMessage
            }

            if (rewardsMap[key] == null && isNewVoting) {
                api.send("Пожалуйста, укажите название награды в квадратных скобках после имени награждаемого", chatId)
                return@voting null
            }
            val senderScreenName = api.getUserNameById(event.userId)
            val screenName = target.targetScreenName
            val votingForMessage = "Голосование за вручение $screenName награды ${rewardsMap[key]}\n" +
                    "Отправь /наградить ${target.rawText}"
            val successVoteMessage = "$senderScreenName проголосовал за награждение $screenName"
            val keyboardMessage = "/наградить ${target.rawText}"
            val onEndVotingMessage = "$screenName получает награду ${rewardsMap[key]}"
            val onTimeIsUp = "Голосование окончено! $screenName, слишком много чести для такой награды!!!"
            Messages(
                votingForMessage = votingForMessage,
                successVoteMessage = successVoteMessage,
                keyboardPositiveMessage = keyboardMessage,
                onEndVotingMessage = onEndVotingMessage,
                onTimeIsUp = onTimeIsUp
            )
        }
    }

    override fun onEndVoting(targetMention: Mention, chatId: Int, api: VkApi) {
        val targetId = targetMention.targetId ?: error("target id is null for mention $targetMention")
        val key = "$chatId.$targetId"
        dbQuery {
            UserReward.insert {
                it[this.chatId] = chatId
                it[userId] = targetId
                it[rewardName] = rewardsMap[key]!!
            }
        }
        rewardsMap.remove(key)
    }

    override var targetNoneMessage: String = "Не указан награждаемый"
    override var targetNoneGetBackMessage: String = ""
    override var targetNullMessage: String = "Товарищ, нельзя наградить того, кого нет"
    override var targetEqualsSenderMessage: String = "Товарищ! Вы не Брежнев"
    override var targetDefendHimSelf: String = ""
    override var targetEqualsBotMessage: String = "Партии не нужны награды, партии нужен только коммунизм!"
    override var alreadyVotedMessage = ", Вы уже проголосовали за награждение этого товарища"
    override var alreadyChoseSide: String = ""

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