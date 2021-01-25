package chatbot.chatModules

import api.*
import api.keyboards.KeyboardBuilder
import api.keyboards.KeyboardButton
import api.keyboards.KeyboardColor
import chatbot.ModuleObject
import chatbot.OnCommand
import chatbot.chatBotEvents.LongPollNewMessageEvent
import database.UserReward
import database.dbQuery
import org.jetbrains.exposed.sql.insert
import java.lang.IllegalArgumentException
import java.lang.StringBuilder

@ModuleObject
object Reward {
    private val rewardVoting = mutableMapOf<Pair<Long, Long>, Voting>()
    private val votedIds = mutableMapOf<Pair<Long, Long>, MutableSet<Long>>()

    private const val coefficientForReward = 0.3 // Процент от онлайна, нужный для награждения
    private const val minCount = 10 // Минимальное кол-во людей для награждения
    private var rewardNameStr = ""

    @OnCommand(["наградить", "reward"], "голосование на отправление в трудовой лагерь")
    fun reward(event: LongPollNewMessageEvent) {
        val api = event.api
        val chatId = event.chatId
        val senderId = event.userId

        val parsed = TextMessageParser().parse(event.text)
        val target = parsed.get<Mention>(1)
        val targetId = target?.targetId
        if (target == null) {
            api.send("Не указан награждаемый", chatId)
            return
        }

        if (targetId == null) {
            api.send("Товарищ, нельзя наградить того, кого нет", chatId)
            return
        }

        if (targetId == senderId) {
            api.send("Товарищ! Вы не Брежнев", chatId)
            return
        }

        if (targetId == api.meId) {
            api.send("Партии не нужны награды, партии нужен только коммунизм!", chatId)
            return
        }

        if (votedIds[targetId to chatId]?.contains(senderId) == true) {
            val senderScreenName = api.getUserNameById(senderId)
            api.send("$senderScreenName, Вы уже проголосовали за награждение этого товарища", chatId)
            return
        }

        val currentTime = event.unixTime
        val screenName = target.targetScreenName

        val isNewVoting = rewardVoting[targetId to chatId] == null ||
                rewardVoting[targetId to chatId]!!.timeOfClosing < currentTime

        val rewardNameInMessage = parsed.getRewardName()
        rewardNameStr = if (rewardNameInMessage != "") rewardNameInMessage else rewardNameStr
        if (rewardNameStr == "" && isNewVoting) {
            api.send("Пожалуйста, укажите название награды в квадратных скобках после имени награждаемого", chatId)
            return
        }

        if (isNewVoting) {
            startNewVoting(senderId, target, chatId, api, rewardNameStr, currentTime)
        } else {
            val votingIsComplete = addVote(senderId, target, chatId, api)
            if (votingIsComplete) {
                endVoting(targetId, screenName, chatId, api)
            }
        }
    }

    private fun startNewVoting(
        senderId: Long,
        target: Mention,
        chatId: Long,
        api: VkPlatform,
        rewardName: String,
        currentTime: Int
    ) {
        if (rewardName == "") throw IllegalArgumentException("Reward name cannot be empty")
        val targetId = target.targetId ?: throw IllegalArgumentException("Target Id cannot be null")
        val screenName = target.targetScreenName
        val onlineCount = getOnlineMemberCount(chatId, api)
        val count = (onlineCount * coefficientForReward).toInt()
        val closingTime = (currentTime + 60 * 5).toLong()
        val newVoting = Voting(closingTime, Integer.max(count, minCount))

        newVoting.addVote(senderId, chatId)
        rewardVoting[targetId to chatId] = newVoting
        votedIds[targetId to chatId] = mutableSetOf(senderId)

        api.send(
            "Голосование за вручение $screenName награды $rewardName - 1/${newVoting.rightNumToVote}\n" +
                    "Отправь /наградить ${target.rawText}",
            chatId
        )
    }

    private fun addVote(senderId: Long, target: Mention, chatId: Long, api: VkPlatform): Boolean {
        val targetId = target.targetId ?: throw IllegalArgumentException("target Id cannot be null")
        val screenName = target.targetScreenName
        val votingIsComplete = rewardVoting[targetId to chatId]!!.addVote(senderId, chatId)
        val senderScreenName = api.getUserNameById(senderId)
        val votedCount = rewardVoting[targetId to chatId]!!.getVotes()
        val necessaryCount = rewardVoting[targetId to chatId]!!.rightNumToVote
        votedIds[targetId to chatId]!!.add(senderId)

        api.send(
            "$senderScreenName проголосовал за награждение $screenName - [$votedCount/$necessaryCount]",
            chatId
        )

        return votingIsComplete
    }

    private fun endVoting(targetId: Long, screenName: String, chatId: Long, api: VkPlatform) {
        dbQuery {
            UserReward.insert {
                it[this.chatId] = chatId
                it[userId] = targetId
                it[rewardName] = rewardNameStr
            }
        }
        api.send("$screenName получает награду $rewardNameStr", chatId)

        votedIds.remove(targetId to chatId)
        rewardNameStr = ""
    }

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

    private fun getOnlineMemberCount(chatId: Long, api: VkPlatform): Int {
        return api.getChatMembers(chatId, listOf("online"))?.count { it.online == 1 } ?: 0
    }
}