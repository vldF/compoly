package chatbot.chatModules.misc

import java.util.concurrent.atomic.AtomicLong


class Voting(
    var timeOfClosing: AtomicLong, // in seconds
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
        timeOfClosing.compareAndSet(timeOfClosing.get(), timeOfClosing.get() + time)
    }

    val completed: Boolean
        get() = voteSet.size >= rightNumToVote
}