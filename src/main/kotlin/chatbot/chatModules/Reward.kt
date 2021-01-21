package chatbot.chatModules

import api.*
import api.keyboards.KeyboardBuilder
import api.keyboards.KeyboardButton
import api.keyboards.KeyboardColor
import chatbot.OnCommand
import chatbot.chatBotEvents.LongPollNewMessageEvent
import java.lang.StringBuilder

object Reward {
    private val rewardVoting = mutableMapOf<Pair<Long, Long>, Voting>()
    private val votedIds = mutableMapOf<Long, MutableSet<Long>>()

    private const val coefficientForReward = 0.3 // Процент от онлайна, нужный для награждения
    private const val minCount = 10 // Минимальное кол-во людей для награждения

    @OnCommand(["наградить", "reward"], "голосование на отправление в трудовой лагерь")
    fun reward(event: LongPollNewMessageEvent) {
        val api = event.api
        val chatId = event.chatId
        val sender = event.userId

        val parsed = TextMessageParser().parse(event.text)
        val target = parsed.get<Mention>(1)
        val rewardName = parsed.getRewardName()
        val targetId = target?.targetId
        if (target == null) {
            api.send("Не указан награждаемый", chatId)
            return
        }

        if (targetId == null) {
            api.send("Товарищ, нельзя наградить того, кого нет", chatId)
            return
        }

        if (targetId == sender) {
            api.send("Товарищ! Вы не Брежнев", chatId)
            return
        }

        if (targetId == api.meId) {
            api.send("Партии не нужны награды, партии нужен только коммунизм!", chatId)
            return
        }

        if (votedIds[targetId]?.contains(sender) == true) {
            val senderScreenName = api.getUserNameById(sender)
            api.send("$senderScreenName, Вы уже проголосовали за награждение этого товарища", chatId)
            return
        }

        val currentTime = System.currentTimeMillis()
        val screenName = target.targetScreenName
        if (rewardVoting[targetId to chatId] == null ||
            rewardVoting[targetId to chatId]!!.timeOfClosing < currentTime) {
            val onlineCount = getOnlineMemberCount(chatId, api)
            val count = (onlineCount * coefficientForReward).toInt()
            val newVoting = Voting(currentTime + 1000 * 60 * 5, Integer.max(count, minCount))

            newVoting.addVote(sender, chatId)
            rewardVoting[targetId to chatId] = newVoting

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
            val votingIsComplete = rewardVoting[targetId to chatId]!!.addVote(sender, chatId)
            val senderScreenName = api.getUserNameById(sender)
            /*api.send(
                "$senderScreenName проголосовал за отправление $screenName в лагерь [${rewardVoting[targetId to chatId]!!.getVotes()}/${Gulag.gulagVoting[targetId to chatId]!!.rightNumToVote}]",
                chatId
            )*/
            if (votingIsComplete) {
                //todo: сделать награды в портофлио
            }
        }

        if (votedIds[targetId] == null) {
            votedIds[targetId] = mutableSetOf(sender)
        } else {
            votedIds[targetId]!!.add(sender)
        }
    }

    private fun ParseObject.getRewardName(): String {
        val textAfterMentionSb = StringBuilder()

        var isRewardName = false
        for (i in 2 until this.size) {
            val word: String = this.get<Text>(i)?.rawText ?: ""
            if (word.first() == '[') {
                isRewardName = true
                word
            }
            if (isRewardName) {
                val correctedWord = word.filter { it != '[' && it != ']' }
                textAfterMentionSb.append(correctedWord)
            }
            if (word.last() == ']') break
        }
        return textAfterMentionSb.toString()
    }

    private fun getOnlineMemberCount(chatId: Long, api: VkPlatform): Int {
        return api.getChatMembers(chatId, listOf("online"))?.count { it.online == 1 } ?: 0
    }
}