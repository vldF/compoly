package modules.chatbot

import api.VkPlatform
import com.google.gson.Gson
import com.google.gson.JsonArray
import group_id
import log
import mainChatPeerId
import modules.chatbot.chatModules.Gulag
import modules.chatbot.chatBotEvents.LongPollEventBase
import modules.chatbot.chatBotEvents.LongPollNewMessageEvent
import modules.chatbot.chatBotEvents.Platform
import useTestChatId
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.ConcurrentLinkedQueue


class VkLongPoll(private val queue: ConcurrentLinkedQueue<LongPollEventBase>): Thread() {
    private lateinit var server: String
    private lateinit var key: String
    private lateinit var ts: String
    private val vk = VkPlatform()


    private fun initLongPoll() {
        val response = vk.post(
                "groups.getLongPollServer",
                mutableMapOf(
                        "group_id" to group_id
                )
        )

        val jsonVK = Gson().fromJson(response, JsonVK::class.java)

        if (jsonVK.error != null || jsonVK.response == null) {
            log.severe("vk long poll connection error. ${jsonVK.error}\n")
            return
        }

        val responseBody = jsonVK.response

        server = responseBody.server
        key = responseBody.key
        ts = responseBody.ts
    }

    private fun longPollRequest(): HttpResponse<String?> {
        val wait = 25
        val request = HttpRequest.newBuilder()
                .uri(URI.create(server))
                .POST(HttpRequest.BodyPublishers.ofString("act=a_check&key=$key&ts=$ts&wait=$wait"))
                .build()
        val client = HttpClient.newHttpClient()
        return try {
            val response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            )
            log.info("response: ${response.body()}")
            response
        } catch (e: IOException) {
            longPollRequest()
        }
    }

    override fun run() {
        initLongPoll()

        while (true) {
            val response = longPollRequest().body()
            val jsonAnswer = Gson().fromJson(response, JsonAnswer::class.java)
            if (jsonAnswer.failed != null) {
                log.severe("long poll error: $response")
                initLongPoll()
                continue
            }

            ts = jsonAnswer.ts

            for (update in jsonAnswer.updates) {
                if (update.type == "message_new" && (!useTestChatId || update.`object`.peer_id != mainChatPeerId)) {
                    val forwarded = update.`object`.reply_message
                    val forwardedFromId = forwarded?.from_id
                    val messageEvent = LongPollNewMessageEvent(
                            Platform.VK,
                            vk,
                            update.`object`.peer_id,
                            update.`object`.text,
                            update.`object`.from_id,
                            forwardedFromId
                    )

                    queue.add(messageEvent)
                }
                // todo refactor
                if (update.type == "message_new" && (update.`object`.action != null)) {
                    if (update.`object`.action.type == "chat_invite_user"
                            || update.`object`.action.type == "chat_invite_user_by_link") {
                        log.info("Somebody was added to chat")
                        val targetId = update.`object`.action.member_id
                        val peerId = update.`object`.peer_id
                        if (Gulag.gulagKickTime.containsKey(targetId to peerId)) {
                            val dif = Gulag.gulagKickTime[targetId to peerId]!! - System.currentTimeMillis()
                            if (dif > 0) {
                                val fullSec = dif / 1000
                                val hours = fullSec / (60 * 60)
                                val minutes = (fullSec % 3600) / 60
                                val seconds = fullSec % 60
                                val message = "Еще наказан ${String.format("%02d:%02d:%02d", hours, minutes, seconds)}"

                                vk.send(message, peerId)
                                sleep(400)
                                vk.kickUserFromChat(targetId, peerId)
                            } else Gulag.gulagKickTime.remove(targetId to peerId)
                        } else vk.send("Приветствуем ${vk.getUserNameById(targetId)}", peerId)
                    }
                }
            }
        }
    }
}

data class JsonVK(val response: Response?, val error: Error?) {
    data class Response(
            val key: String,
            val server: String,
            val ts: String
    )

    data class Error(
            val error_code: Int,
            val error_msg: String,
            val request_params: JsonArray
    )
}

data class JsonAnswer(
        val ts: String,
        val updates: List<UpdatePart>,
        val failed: Int?
) {
    data class UpdatePart(
            val type: String,
            val `object`: MessageNewObj,
            val group_id: Int,
            val event_id: String
    )

}

data class MessageNewObj(
        val date: Int,
        val from_id: Long,
        val id: Long,
        val out: Int,
        val peer_id: Long,
        val text: String,
        val conversation_message_id: Int,
        val reply_message: MessageNewObj?,
        val important: Boolean,
        val random_id: Int,
        val attachments: List<Any>,
        val is_hidden: Boolean,
        val action: Action?
) {
    data class Action(
            val type: String,
            val member_id: Long,
            val text: String,
            val email: String,
            val photo: Any
    )
}