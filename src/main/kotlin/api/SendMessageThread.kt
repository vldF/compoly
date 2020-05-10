package api
import log

object SendMessageThread: Thread() {

    private var listMessages: MutableList<Pair<String, List<String>>> = mutableListOf()
    private const val maxMessagesInOneSession = 7

    override fun run() {
        while (true) {
            if (listMessages.isNotEmpty()) {
                var count = 0
                while (listMessages.isNotEmpty()) {
                    val text = listMessages[0].first
                    val chatIds = listMessages[0].second
                    listMessages.removeAt(0)
                    for (id in chatIds) {
                        log.info(text)
                        count++
                        Vk().post("messages.send", mutableMapOf(
                                "message" to text,
                                "chat_id" to id))
                        if (count == maxMessagesInOneSession) {
                            sleep(3000)
                            count = 0
                        }
                    }
                }
            }
            sleep(200)
        }
    }

    fun addInList(message: String, chatId: List<String>) {
        listMessages.add(message to chatId)
    }
}