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
    override fun voting(event: LongPollNewMessageEvent) {
        val api = event.api
        val chatId = event.chatId

        val parsed = TextMessageParser().parse(event.text)
        val target = parsed.get<Mention>(1)
        if (target != null) {
            val targetId = target.targetId
            val isNewVoting =
                voting[targetId to chatId] == null || voting[targetId to chatId]!!.timeOfClosing < event.time
            val rewardNameInMessage = parsed.getRewardName()
            rewardNameStr = if (rewardNameInMessage != "") rewardNameInMessage else rewardNameStr
            if (rewardNameStr == "" && isNewVoting) {
                api.send("Пожалуйста, укажите название награды в квадратных скобках после имени награждаемого", chatId)
                return
            }
            val screenName = target.targetScreenName

            votingForMessage = "Голосование за вручение $screenName награды $rewardNameStr\n" +
                    "Отправь /наградить ${target.rawText}"

            successVoteMessage = "за награждение $screenName"

            keyboardMessage = "/наградить ${target.rawText}"

            onEndVotingMessage = "$screenName получает награду $rewardNameStr"
        }
        super.voting(event)
    }

    override fun onEndVoting(targetId: Int, chatId: Int, api: VkPlatform) {
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
        val textAfterMentionSb = StringBuilder()

        var isRewardName = false
        for (i in 2 until this.size) {
            val word: String = this.get<Text>(i)?.rawText ?: ""
            if (word.first() == '[') {
                isRewardName = true
            }
            if (isRewardName) {
                val correctedWord = word.filter { it != '[' && it != ']' }
                textAfterMentionSb.append("$correctedWord ")
            }
            if (word.last() == ']') break
        }

        return textAfterMentionSb.trimEnd().toString()
    }
}