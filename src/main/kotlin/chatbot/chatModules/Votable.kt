package chatbot.chatModules

import api.Mention
import api.TextMessageParser
import api.VkPlatform
import api.keyboards.KeyboardBuilder
import api.keyboards.KeyboardButton
import api.keyboards.KeyboardColor
import botId
import chatbot.chatBotEvents.LongPollNewMessageEvent
import chatbot.chatModules.misc.Voting
import log

/**Something that able to vote for*/
abstract class Votable {
    /**Map of votes: (targetId to chatId) to Voting*/
    protected val voting = mutableMapOf<Pair<Int, Int>, Voting>()

    /**Map of...*/
    protected val votedIds = mutableMapOf<Pair<Int, Int>, MutableSet<Int>>()

    /**The percentage of online required to win the vote*/
    protected open var percentageOfOnline = 0.3

    /**The minimum number of people to win the vote*/
    protected open var minCount = 10

    /**Time until the end of voting in minutes*/
    protected open var timeOfClosing = 60 * 5

    /**Return the number of people who are online*/
    private fun getOnlineMemberCount(chatId: Int, api: VkPlatform): Int {
        return api.getChatMembers(chatId, listOf("online"))?.count { it.online == 1 } ?: 0
    }

    /**Start a new voting for [votingForMessage]
     *
     * [senderId] - initiator
     *
     * [target] - whose fate is being decided
     *
     * [chatId] - chat id
     *
     * [api] - vk api
     *
     * [currentTime] - voting start time*/
    private fun startNewVoting(senderId: Int, target: Mention, chatId: Int, api: VkPlatform, currentTime: Long) {
        val targetId = target.targetId!!
        val onlineCount = getOnlineMemberCount(chatId, api)
        val count = (onlineCount * percentageOfOnline).toInt()
        val newVoting =
            Voting(timeOfClosing = currentTime + timeOfClosing, rightNumToVote = Integer.max(count, minCount))

        log.info("Start new voting: $votingForMessage")

        newVoting.addVote(senderId, chatId)
        voting[targetId to chatId] = newVoting
        votedIds[targetId to chatId] = mutableSetOf(senderId)

        val keyboard = KeyboardBuilder()
            .addButton(KeyboardButton(keyboardMessage, "за", KeyboardColor.NEGATIVE))
            .build()
        val split = votingForMessage.split("\n")
        api.send(
            "${split[0]} - 1/${newVoting.rightNumToVote}\n${split.drop(1).joinToString("")}",
            chatId,
            keyboard = keyboard
        )
    }

    /**Add [senderId] vote*/
    private fun addVote(senderId: Int, target: Mention, chatId: Int, api: VkPlatform): Boolean {
        val targetId = target.targetId!!
        val votingIsComplete = voting[targetId to chatId]!!.addVote(senderId, chatId)
        val senderScreenName = api.getUserNameById(senderId)
        val votedCount = voting[targetId to chatId]!!.getVotes()
        val necessaryCount = voting[targetId to chatId]!!.rightNumToVote
        votedIds[targetId to chatId]!!.add(senderId)

        val message = "$senderScreenName проголосовал $successVoteMessage - [$votedCount/$necessaryCount]"

        log.info("New vote - $message")

        api.send(message, chatId)

        return votingIsComplete
    }

    /**Called when enough people have voted*/
    private fun endVoting(targetId: Int, chatId: Int, api: VkPlatform) {
        onEndVoting(targetId, chatId, api)
        log.info("Voting ends: $onEndVotingMessage")
        api.send(onEndVotingMessage, chatId)
        votedIds.remove(targetId to chatId)
        voting[targetId to chatId]
    }

    /**Special action at the end of a vote*/
    protected abstract fun onEndVoting(targetId: Int, chatId: Int, api: VkPlatform)

    /**No target*/
    protected abstract var targetNoneMessage: String

    /**No target when trying to invalidate voting results*/
    protected abstract var targetNoneGetBackMessage: String

    /**Target is null*/
    protected abstract var targetNullMessage: String

    /**Target is sender*/
    protected abstract var targetEqualsSenderMessage: String

    /**Target is bot*/
    protected abstract var targetEqualsBotMessage: String

    /**Sender already voted to current voting*/
    protected abstract var alreadyVotedMessage: String

    /**What we vote for*/
    protected lateinit var votingForMessage: String

    /**Successfully voted*/
    protected lateinit var successVoteMessage: String

    /**Keyboard message*/
    protected lateinit var keyboardMessage: String

    /**Post-voting message*/
    protected lateinit var onEndVotingMessage: String

    /**Voting process
     *
     * Override this fun and use [super.voting(event)]
     * @sample Gulag.voting
     */
    open fun voting(event: LongPollNewMessageEvent) {
        voting(event, false)
    }

    /**Admin voting process*/
    open fun adminVoting(event: LongPollNewMessageEvent) {
        voting(event, true)
    }

    /**If you want to cancel the voting results*/
    open fun cancelVotingResult(event: LongPollNewMessageEvent) {
        val api = event.api
        val chatId = event.chatId
        val sender = event.userId

        val parsed = TextMessageParser().parse(event.text)
        val target = parsed.get<Mention>(1)
        val targetId = target?.targetId
        if (target == null) {
            api.send(targetNoneGetBackMessage, chatId)
            return
        }

        if (targetId == null) {
            api.send(targetNullMessage, chatId)
            return
        }

        if (targetId == sender) {
            api.send(targetEqualsSenderMessage, chatId)
            return
        }
    }

    /**Private voting process*/
    private fun voting(event: LongPollNewMessageEvent, admin: Boolean) {
        val api = event.api
        val chatId = event.chatId
        val senderId = event.userId

        val parsed = TextMessageParser().parse(event.text)
        val target = parsed.get<Mention>(1)
        val targetId = target?.targetId
        if (target == null) {
            api.send(targetNoneMessage, chatId)
            return
        }

        if (targetId == null) {
            api.send(targetNullMessage, chatId)
            return
        }

        if (targetId == senderId) {
            api.send(targetEqualsSenderMessage, chatId)
            return
        }

        if (targetId == botId) {
            api.send(targetEqualsBotMessage, chatId)
            return
        }

        if (votedIds[targetId to chatId]?.contains(senderId) == true) {
            val senderScreenName = api.getUserNameById(senderId)
            api.send("$senderScreenName$alreadyVotedMessage", chatId)
            return
        }
        if (admin) return
        val currentTime = event.time

        val isNewVoting = voting[targetId to chatId] == null
                || voting[targetId to chatId]!!.timeOfClosing < currentTime
                || voting[targetId to chatId]!!.completed

        if (isNewVoting) {
            startNewVoting(senderId, target, chatId, api, currentTime)
        } else {
            val votingIsComplete = addVote(senderId, target, chatId, api)
            if (votingIsComplete) {
                endVoting(targetId, chatId, api)
            }
        }
    }

}