package chatbot.chatModules.misc

import java.util.concurrent.atomic.AtomicLong


class Voting(
    var timeOfClosing: AtomicLong, // in ms
    var rightNumToVote: Int
) {
    private val voteSet = mutableSetOf<Pair<Int, Int>>()

    var isFinishedSuccessful = false

    fun addVote(id: Int, peer_id: Int): Boolean {
        voteSet += id to peer_id
        if (voteSet.size >= rightNumToVote) isFinishedSuccessful = true
        return isFinishedSuccessful
    }

    fun increaseRightNumToVote(count: Int) {
        rightNumToVote += count
    }

    fun getVotes() = voteSet.size

    fun increaseTimeOfClosing(time: Long) {
        timeOfClosing.set(timeOfClosing.get() + time)
    }

    val isTimeUp: Boolean
        get() = System.currentTimeMillis() >= timeOfClosing.get()
}