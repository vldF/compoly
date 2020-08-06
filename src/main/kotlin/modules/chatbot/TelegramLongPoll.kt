package modules.chatbot

import api.*
import modules.chatbot.chatBotEvents.*
import java.util.concurrent.ConcurrentLinkedQueue

class TelegramLongPoll(
        private val queue: ConcurrentLinkedQueue<LongPollEventBase>
): Thread() {
    private val telegram = TelegramPlatform
    override fun run() {
        var lastUpdateId = 0
        while(true) {
            val updates = telegram.getUpdates(lastUpdateId + 1) ?: continue
            if(updates.size > 10) {
                lastUpdateId = updates.last().update_id
                continue
            }
            for (update in updates) {
                lastUpdateId = update.update_id
                processMessage(update.message)
                processPollAnswer(update.poll_answer)
                processPoll(update.poll)
            }
        }
    }

    fun processMessage(message: TGMessage?) {
        if (message == null) return
        if (message?.from?.username != null) {
            TelegramUsersDataBase.addId(message.from.id, message.from.username)
        }
        val messageEvent = LongPollNewTGMessageEvent(
                Platform.TELEGRAM,
                telegram,
                message.chat.id,
                message.text ?: "/${message.dice?.emoji}" ?: "",
                message.from.id,
                message.dice?.value,
                message.reply_to_message?.from?.id,
                message.poll?.id
        )
        queue.add(messageEvent)
    }

    fun processPollAnswer(pollAnswer: TGPollAnswer?) {
        if (pollAnswer == null) return
        if (pollAnswer?.user.username != null) {
            TelegramUsersDataBase.addId(pollAnswer.user.id, pollAnswer.user.username)
        }
        val pollAnswerEvent = LongPollNewPollAnswerEvent(
                Platform.TELEGRAM,
                telegram,
                pollAnswer.user.id,
                pollAnswer.poll_id,
                pollAnswer.option_ids
        )
        queue.add(pollAnswerEvent)
    }

    fun processPoll(poll: TGPoll?) {
        if (poll == null) return
        val pollEvent = LongPollEventNewPoll(
                Platform.TELEGRAM,
                telegram,
                poll.id
        )
        queue.add(pollEvent)
    }
}


