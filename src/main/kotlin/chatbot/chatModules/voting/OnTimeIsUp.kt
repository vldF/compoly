package chatbot.chatModules.voting

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import log
import modules.events.EventStream
import java.util.concurrent.atomic.AtomicLong


/** Event for callback Votable.onTimeIsUp*/
class OnTimeIsUp(
    private val dynamicDelay: AtomicLong,
    private val targetId: Int,
    private val chatId: Int,
    private val messages: Votable.Messages,
    private val votable: Votable
) {

    fun listen() {
        EventStream.addDynamicTask {
            var time = calculateDelayTime(dynamicDelay.get(), System.currentTimeMillis())

            runBlocking {
                while (time > 0) {
                    log.info("Sleeping for $time until next <${votable}> call")
                    delay(time)
                    time = calculateDelayTime(dynamicDelay.get(), System.currentTimeMillis())
                }
            }

            votable.onTimeIsUp(targetId, chatId, messages)
        }
    }

    private fun calculateDelayTime(eventTime: Long, currentTime: Long): Long {
        return eventTime - currentTime
    }

}