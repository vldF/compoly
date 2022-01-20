package chatbot.chatModules.voting

import chatbot.chatModules.misc.Voting
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import log
import modules.events.EventStream


/** Event for callback Votable.onTimeIsUp*/
class OnTimeIsUp(
    private val voting: Voting,
    private val targetId: Int,
    private val chatId: Int,
    private val messages: Votable.Messages,
    private val votable: Votable
) {

    fun call() {
        EventStream.addDynamicTask {
            var time = calculateDelayTime(voting.timeOfClosing * 1000, System.currentTimeMillis())

            runBlocking {
                while (time > 0) {
                    log.info("Sleeping for $time until next <${votable}> call")
                    delay(time)
                    time = calculateDelayTime(voting.timeOfClosing * 1000, System.currentTimeMillis())
                }
            }

            votable.onTimeIsOut(targetId, chatId, messages)
        }
    }

    private fun calculateDelayTime(eventTime: Long, currentTime: Long): Long {
        return eventTime - currentTime
    }

}