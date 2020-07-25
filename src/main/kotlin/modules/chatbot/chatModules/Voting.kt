package modules.chatbot.chatModules


class Voting(
    val timeOfClosing: Long,
    val rightNumToVote: Int
) {
    private val voteSet = mutableSetOf<Pair<Long, Long>>()

    fun addVote(id: Long, peer_id: Long): Boolean {
        voteSet += id to peer_id
        return voteSet.size >= rightNumToVote
    }

    fun getVotes() = voteSet.size
}