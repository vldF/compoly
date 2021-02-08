package chatbot.chatModules.misc


class Voting(
    val timeOfClosing: Long,
    val rightNumToVote: Int
) {
    private val voteSet = mutableSetOf<Pair<Int, Int>>()

    fun addVote(id: Int, peer_id: Int): Boolean {
        voteSet += id to peer_id
        return voteSet.size >= rightNumToVote
    }

    fun getVotes() = voteSet.size

    val completed: Boolean
        get() = voteSet.size >= rightNumToVote
}