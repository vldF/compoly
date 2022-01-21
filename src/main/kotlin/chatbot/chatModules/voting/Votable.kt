package chatbot.chatModules.voting

import api.GarbageMessage.Companion.toGarbageMessageWithDelay
import api.GarbageMessagesCollector
import api.GarbageMessagesCollector.Companion.DEFAULT_DELAY
import api.GarbageMessagesCollector.Companion.MINUTE_DELAY
import api.Mention
import api.TextMessageParser
import api.VkApi
import api.keyboards.KeyboardBuilder
import api.keyboards.KeyboardButton
import api.keyboards.KeyboardColor
import chatbot.chatBotEvents.LongPollNewMessageEvent
import chatbot.chatModules.misc.Voting
import configs.botId
import log
import modules.events.EventStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**Something that able to vote for*/
abstract class Votable {
    /**Map of votes: (targetId to chatId) to Voting*/
    protected val voting = mutableMapOf<Pair<Int, Int>, Voting>()

    /**Map of...*/
    private val votedIds = mutableMapOf<Pair<Int, Int>, MutableSet<Int>>()

    /**The percentage of online required to win the vote*/
    protected open var percentageOfOnline = 0.3

    /**The minimum number of people to win the vote*/
    protected open var minCount = 12

    /**Time until the end of voting in ms*/
    protected open var timeOfClosing = 60 * 5 * 1000L

    private val targets = ConcurrentHashMap<Int, MutableSet<Int>>()

    /**Return the number of people who are online*/
    private fun getOnlineMemberCount(chatId: Int, api: VkApi): Int {
        return api.getChatMembers(chatId, listOf("online"))?.count { it.online == 1 } ?: 0
    }

    /**Start a new voting for [Messages.votingForMessage]
     *
     * [senderId] - initiator
     *
     * [target] - whose fate is being decided
     *
     * [chatId] - chat id
     *
     * [api] - vk api */
    private fun startNewVoting(
        senderId: Int,
        target: Mention,
        chatId: Int,
        api: VkApi,
        messages: Messages
    ) {
        val targetId = target.targetId!!
        val onlineCount = getOnlineMemberCount(chatId, api)
        val count = (onlineCount * percentageOfOnline).toInt()
        val newVoting =
            Voting(
                timeOfClosing = AtomicLong(System.currentTimeMillis() + timeOfClosing),
                rightNumToVote = Integer.max(count, minCount)
            )

        log.info("Start new voting: ${messages.votingForMessage}")

        newVoting.addVote(senderId, chatId)
        voting[targetId to chatId] = newVoting
        votedIds[targetId to chatId] = mutableSetOf(senderId)

        val keyboard = KeyboardBuilder()
            .addButton(KeyboardButton(messages.keyboardPositiveMessage, "за", KeyboardColor.POSITIVE))

        if (messages.keyboardNegativeMessage != "") {
            keyboard.addButton(KeyboardButton(messages.keyboardNegativeMessage, "против", KeyboardColor.NEGATIVE))
        }

        onTimeIsUpThreadRunner(newVoting.timeOfClosing, targetId, chatId, messages)

        val split = messages.votingForMessage.split("\n")
        api.send(
            "${split[0]} - 1/${newVoting.rightNumToVote}\n${split.drop(1).joinToString("")}",
            chatId,
            keyboard = keyboard.build(),
            dynamicRemoveTime = newVoting.timeOfClosing
        )
    }

    /**Add [senderId] vote*/
    private fun addVote(senderId: Int, target: Mention, chatId: Int, api: VkApi, messages: Messages): Boolean {
        val targetId = target.targetId!!
        val currentVoting = voting[targetId to chatId]!!

        val votingIsComplete = currentVoting.addVote(senderId, chatId)
        currentVoting.increaseTimeOfClosing(MINUTE_DELAY)
        val senderScreenName = api.getUserNameById(senderId)
        val votedCount = currentVoting.getVotes()
        val necessaryCount = currentVoting.rightNumToVote
        votedIds[targetId to chatId]!!.add(senderId)

        val message = "$senderScreenName проголосовал ${messages.successVoteMessage} - [$votedCount/$necessaryCount]"

        log.info("New vote - $message")

        api.send(message, chatId, dynamicRemoveTime = voting[targetId to chatId]!!.timeOfClosing)

        return votingIsComplete
    }

    /**Increase necessary count for finish [senderId] vote*/
    private fun increaseNecessaryCountVote(
        targetId: Int,
        senderId: Int,
        chatId: Int,
        api: VkApi,
        messages: Messages
    ) {
        val currentVoting = voting[targetId to chatId]!!

        currentVoting.increaseRightNumToVote(1)
        val senderScreenName = api.getUserNameById(senderId)
        val votedCount = currentVoting.getVotes()
        val necessaryCount = currentVoting.rightNumToVote
        votedIds[targetId to chatId]!!.add(senderId)

        val message = "$senderScreenName проголосовал ${messages.successVoteMessage} - [$votedCount/$necessaryCount]"

        log.info("New vote - $message")

        api.send(message, chatId, dynamicRemoveTime = voting[targetId to chatId]!!.timeOfClosing)
    }

    /**Called when enough people have voted*/
    private fun endVoting(targetId: Int, chatId: Int, api: VkApi, messages: Messages) {
        log.info("Voting ends: ${messages.onEndVotingMessage}")
        api.send(messages.onEndVotingMessage, chatId)
        onEndVoting(targetId, chatId, api)
        votedIds.remove(targetId to chatId)
    }

    /**Special action at the end of a vote*/
    protected abstract fun onEndVoting(targetId: Int, chatId: Int, api: VkApi)

    /**No target*/
    protected abstract var targetNoneMessage: String

    /**No target when trying to invalidate voting results*/
    protected abstract var targetNoneGetBackMessage: String

    /**Target is null*/
    protected abstract var targetNullMessage: String

    /**Target is sender*/
    protected abstract var targetEqualsSenderMessage: String

    /**Target defend himself*/
    protected abstract var targetDefendHimSelf: String

    /**Target is bot*/
    protected abstract var targetEqualsBotMessage: String

    /**Sender already voted to current voting*/
    protected abstract var alreadyVotedMessage: String

    protected abstract var alreadyChoseSide: String

    /**Voting process
     *
     * @param someActions dynamic creation of sent messages depending on the event (see [Messages])
     * @sample Gulag.voting
     */
    fun voting(
        event: LongPollNewMessageEvent,
        someActions: (api: VkApi, chatId: Int, senderId: Int, target: Mention) -> Messages?
    ) {
        voting(event, null, someActions)
    }

    /**Voting process
     *
     * @param someActions dynamic creation of sent messages depending on the event (see [Messages])
     * @sample Gulag.justifyMember
     */
    fun votingAgainst(
        event: LongPollNewMessageEvent,
        someActions: (api: VkApi, chatId: Int, senderId: Int, target: Mention) -> Messages?
    ) {
        votingAgainst(event, null, someActions)
    }

    /**Admin voting process
     *
     * @param adminActions dynamic creation of sent admin-messages depending on the event (see [Messages])
     * */
    fun adminVoting(
        event: LongPollNewMessageEvent,
        adminActions: (api: VkApi, chatId: Int, senderId: Int, target: Mention) -> Messages
    ) {
        voting(event, adminActions) { _, _, _, _ -> Messages() }
    }

    /**If you want to cancel the voting results*/
    fun cancelVotingResult(
        event: LongPollNewMessageEvent,
        targetEqualsSender: String,
        cancelAction: ((api: VkApi, chatId: Int, senderId: Int, target: Mention) -> Unit)?
    ) {
        val api = event.api
        val chatId = event.chatId
        val senderId = event.userId

        val parsed = TextMessageParser().parse(event.text)
        val target = parsed.get<Mention>(1)
        val targetId = target?.targetId
        if (target == null) {
            api.send(targetNoneGetBackMessage, chatId, removeDelay = DEFAULT_DELAY)
            return
        }

        if (targetId == null) {
            api.send(targetNoneGetBackMessage, chatId, removeDelay = DEFAULT_DELAY)
            return
        }

        if (targetId == senderId) {
            api.send(targetEqualsSender, chatId, removeDelay = DEFAULT_DELAY)
            return
        }

        if (cancelAction != null) cancelAction(api, chatId, senderId, target)
        GarbageMessagesCollector.addGarbageMessage(event.toGarbageMessageWithDelay(DEFAULT_DELAY))
    }

    private fun onTimeIsUpThreadRunner(dynamicDelay: AtomicLong, targetId: Int, chatId: Int, messages: Messages) {
        EventStream.addDynamicTask {
            var time = calculateDelayTime(dynamicDelay.get(), System.currentTimeMillis())
            while (time > 0) {
                log.info("Sleeping for $time until next <${this}> call")
                Thread.sleep(time)
                time = calculateDelayTime(dynamicDelay.get(), System.currentTimeMillis())
            }
            onTimeIsUp(targetId, chatId, messages)
        }
    }

    private fun calculateDelayTime(eventTime: Long, currentTime: Long): Long {
        return eventTime - currentTime
    }

    private fun onTimeIsUp(
        targetId: Int,
        chatId: Int,
        message: Messages
    ) {
        log.info("Voting time is up: ${message.onTimeIsUp}")
        votedIds.remove(targetId to chatId)
        if (!voting[targetId to chatId]!!.isFinishedSuccessful && message.onTimeIsUp != "") {
            VkApi.send(message.onTimeIsUp, chatId, removeDelay = MINUTE_DELAY)
        }
    }

    /**Private voting process*/
    private fun voting(
        event: LongPollNewMessageEvent,
        adminActions: ((api: VkApi, chatId: Int, senderId: Int, target: Mention) -> Messages)?,
        someActions: (api: VkApi, chatId: Int, senderId: Int, target: Mention) -> Messages?
    ) {
        val api = event.api
        val chatId = event.chatId
        val senderId = event.userId

        val parsed = TextMessageParser().parse(event.text)
        val target = parsed.get<Mention>(1)
        val targetId = target?.targetId
        if (target == null) {
            api.send(targetNoneMessage, chatId, removeDelay = DEFAULT_DELAY)
            return
        }

        if (targetId == botId) {
            api.send(targetEqualsBotMessage, chatId, removeDelay = DEFAULT_DELAY)
            return
        }

        if (targetId == senderId) {
            api.send(targetEqualsSenderMessage, chatId, removeDelay = DEFAULT_DELAY)
            return
        }

        var messages = someActions(api, chatId, senderId, target) ?: return

        val currentVoting = voting[targetId to chatId]

        val isNeedNewVote = currentVoting == null
                || currentVoting.isTimeUp
                || currentVoting.isFinishedSuccessful


        GarbageMessagesCollector.addGarbageMessage(event.toGarbageMessageWithDelay(DEFAULT_DELAY))

        if ((targetId == null || api.getChatMembers(chatId, emptyList())
                ?.find { it.id == targetId } == null) && !targets.getOrPut(chatId) { mutableSetOf() }
                .contains(targetId)
        ) {
            if (targetId != null && votedIds[targetId to chatId]?.contains(senderId) == true) {
                if (isNeedNewVote) {
                    startNewVoting(senderId, target, chatId, api, messages)
                    return
                }
                val senderScreenName = api.getUserNameById(senderId)
                api.send("$senderScreenName$alreadyVotedMessage", chatId, removeDelay = DEFAULT_DELAY)
            } else api.send(targetNullMessage, chatId, removeDelay = DEFAULT_DELAY)
            return
        }

        if (votedIds[targetId to chatId]?.contains(senderId) == true) {
            if (isNeedNewVote) {
                startNewVoting(senderId, target, chatId, api, messages)
                return
            }
            val senderScreenName = api.getUserNameById(senderId)
            api.send("$senderScreenName$alreadyVotedMessage", chatId, removeDelay = DEFAULT_DELAY)
            return
        }

        if (adminActions != null) {
            messages = adminActions(api, chatId, senderId, target)
            endVoting(targetId!!, chatId, api, messages)
            return
        }

        if (isNeedNewVote) {
            startNewVoting(senderId, target, chatId, api, messages)
        } else {
            val votingIsComplete = addVote(senderId, target, chatId, api, messages)
            if (votingIsComplete) {
                targets.getOrPut(chatId) { mutableSetOf() }.add(targetId!!)
                endVoting(targetId, chatId, api, messages)
            }
        }
    }

    /**Private voting against process*/
    private fun votingAgainst(
        event: LongPollNewMessageEvent,
        adminActions: ((api: VkApi, chatId: Int, senderId: Int, target: Mention) -> Messages)?,
        someActions: (api: VkApi, chatId: Int, senderId: Int, target: Mention) -> Messages?
    ) {
        val api = event.api
        val chatId = event.chatId
        val senderId = event.userId

        val parsed = TextMessageParser().parse(event.text)
        val target = parsed.get<Mention>(1)
        val targetId = target?.targetId

        if (target == null || targetId == null) {
            api.send(targetNoneGetBackMessage, chatId, removeDelay = DEFAULT_DELAY)
            return
        }

        if (targetId == senderId) {
            api.send(targetDefendHimSelf, chatId, removeDelay = DEFAULT_DELAY)
            return
        }

        GarbageMessagesCollector.addGarbageMessage(event.toGarbageMessageWithDelay(DEFAULT_DELAY))

        val messages = someActions(api, chatId, senderId, target) ?: return

        val currentVotedIds = votedIds[targetId to chatId]!!

        if (currentVotedIds.contains(senderId)) {
            val senderScreenName = api.getUserNameById(senderId)
            api.send("$senderScreenName$alreadyChoseSide", chatId, removeDelay = DEFAULT_DELAY)
            return
        }

        increaseNecessaryCountVote(targetId, senderId, chatId, api, messages)
    }

    data class Messages(
        /**What we vote for*/
        val votingForMessage: String = "",

        /**Successfully voted*/
        val successVoteMessage: String = "",

        /**Keyboard positive message*/
        val keyboardPositiveMessage: String = "",

        /**Keyboard negative message*/
        val keyboardNegativeMessage: String = "",

        /**Voted successfully end*/
        val onEndVotingMessage: String = "",

        /**Vote time is up*/
        val onTimeIsUp: String = ""
    )

}