package chatbot.chatModules.misc


class Voting(
    @Volatile var timeOfClosing: Long, // in seconds
    var rightNumToVote: Int
) {
    private val voteSet = mutableSetOf<Pair<Int, Int>>()

    fun addVote(id: Int, peer_id: Int): Boolean {
        voteSet += id to peer_id
        return voteSet.size >= rightNumToVote
    }

    fun increaseRightNumToVote(count: Int) {
        rightNumToVote += count
    }

    fun getVotes() = voteSet.size

    fun increaseTimeOfClosing(time: Long) {
        timeOfClosing += time
    }

    val completed: Boolean
        get() = voteSet.size >= rightNumToVote
}