package chatbot

import api.GarbageMessagesCollector.Companion.MINUTE_DELAY
import api.VkApi
import com.google.gson.Gson
import com.google.gson.JsonArray
import configs.botId
import log
import configs.mainChatPeerId
import chatbot.chatModules.voting.Gulag
import chatbot.chatBotEvents.LongPollEventBase
import chatbot.chatBotEvents.LongPollNewMessageEvent
import chatbot.chatModules.voting.Mute
import configs.useTestChatId
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.ConcurrentLinkedQueue


class LongPoll(private val queue: ConcurrentLinkedQueue<LongPollEventBase>) : Thread() {
    private lateinit var server: String
    private lateinit var key: String
    private lateinit var ts: String


    private fun initLongPoll() {
        val response = VkApi.post(
            "groups.getLongPollServer",
            mutableMapOf(
                "group_id" to botId
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

            // todo refactor
            for (update in jsonAnswer.updates) {
                if (update.type == "message_new" && (!useTestChatId || update.`object`.peer_id != mainChatPeerId)) {
                    val replyMessage = update.`object`.reply_message
                    var replyFromId = replyMessage?.from_id

                    val forwarded = update.`object`.fwd_messages
                    val forwardedFromId = forwarded.firstOrNull()?.from_id
                    if (forwarded.all { it.from_id == forwardedFromId }) {
                        if (replyFromId == null) {
                            replyFromId = forwardedFromId
                        }
                    }

                    val callback = Gson().fromJson(update.`object`.payload, Callback::class.java)
                    val text = callback?.callback ?: update.`object`.text

                    val messageEvent = LongPollNewMessageEvent(
                        VkApi,
                        update.`object`.peer_id,
                        text,
                        update.`object`.from_id,
                        replyFromId,
                        update.`object`.date.toLong(),
                        update.`object`.attachments,
                        update.`object`.conversation_message_id
                    )

                    val targetId = messageEvent.userId
                    val peerId = messageEvent.chatId
                    if (Mute.mutedTime.containsKey(targetId to peerId)) {
                        val dif = Mute.mutedTime[targetId to peerId]!! - System.currentTimeMillis()
                        if (dif > 0) {
                            VkApi.deleteMessage(messageEvent.messageId, messageEvent.chatId)
                            continue
                        } else {
                            Mute.mutedTime.remove(targetId to peerId)
                            VkApi.send("Больше не шали, $targetId.", peerId, removeDelay = MINUTE_DELAY)
                        }
                    }

                    queue.add(messageEvent)
                }

                if (update.type == "message_new" && (update.`object`.action != null)) {
                    if (update.`object`.action.type == "chat_invite_user"
                        || update.`object`.action.type == "chat_invite_user_by_link"
                    ) {
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

                                VkApi.send(message, peerId)
                                sleep(400)
                                VkApi.kickUserFromChat(peerId, targetId)
                            } else {
                                Gulag.gulagKickTime.remove(targetId to peerId)
                            }
                        } else {
                            if (targetId > 0) {
                                VkApi.send("Приветствуем ${VkApi.getUserNameById(targetId)}", peerId)
                            }
                        }
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
    val updates: List<Update>,
    val failed: Int?
)

data class Update(
    val type: String,
    val `object`: MessageNewObj,
    val group_id: Int,
    val event_id: String
)

data class MessageNewObj(
    val date: Int,
    val from_id: Int,
    val id: Long,
    val out: Int,
    val peer_id: Int,
    val text: String,
    val conversation_message_id: Int,
    val reply_message: MessageNewObj?,
    val important: Boolean,
    val random_id: Int,
    val attachments: List<Attachment>,
    val is_hidden: Boolean,
    val action: Action?,
    val payload: String,
    val fwd_messages: Array<MessageNewObj>
)

data class Attachment(
    val type: String,
    val photo: Photo? = null,
    val video: AttachmentObj? = null,
    val audio: AttachmentObj? = null,
    val doc: Doc? = null,
    val poll: Poll? = null
)

data class Photo(
    val id: Int,
    val owner_id: Int,
    val access_key: String? = null,
    val sizes: List<Size>? = null
)

data class Doc(
    val id: Int,
    val owner_id: Int,
    val access_key: String? = null,
    val title: String,
    val ext: String,
    val url: String
)

data class Size(
    val height: Int,
    val url: String,
    val type: String,
    val width: Int
)

data class Poll(
    val owner_id: Int,
    val id: Int,
    val access_key: String? = null
)

data class AttachmentObj(
    val id: Int,
    val owner_id: Int,
    val access_key: String? = null
)

data class Action(
    val type: String,
    val member_id: Int,
    val text: String,
    val email: String,
    val photo: Any
)

data class Callback(
    val callback: String
)