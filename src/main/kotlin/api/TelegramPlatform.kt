package api

import com.google.gson.Gson
import log
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.impl.client.HttpClientBuilder
import telApiToken
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

object TelegramPlatform : PlatformApiInterface {
    private val gson = Gson()
    private val client = HttpClient.newHttpClient()
    private val chatIds = setOf<Long>(-445009017)
    private const val token = telApiToken

    override val meId: Long by lazy { getMe()?.id ?: 0 }

    override fun send(text: String, chatId: Long, pixUrls: List<String>) {
        if(pixUrls.isEmpty()) sendMessage(chatId, text)
        else {
            if (pixUrls.size == 1) sendPhotoURL(chatId, pixUrls[0], text)
            else sendMediaGroupURL(
                    chatId,
                    pixUrls.map { TGInputMedia("photo", it) }.toTypedArray()
            )
        }
    }

    override fun getUserNameById(id: Long): String? {
        for (chatId in chatIds) {
            val values = mapOf(
                    "chat_id" to chatId,
                    "user_id" to id
            )
            val result =
                    makeJsonRequest<ChatMemberResponse>("getChatMember", values)
            if (result != null) return (result as TGChatMember).user.username
        }
        return null
    }

    override fun getUserIdByName(username: String): Long? = TelegramUsersDataBase.getIdByNick(username)

    override fun kickUserFromChat(chatId: Long, userId: Long) {
        val values = mapOf(
                "chat_id" to chatId,
                "user_id" to userId
        )
        makeJsonRequest<Boolean>("kickChatMember", values)
    }

    override fun isUserAdmin(chatId: Long, userId: Long): Boolean {
        val values = mapOf(
                "chat_id" to chatId,
                "user_id" to userId
        )

        val result = makeJsonRequest<ChatMemberResponse>("getChatMember", values)
        val status = (result as TGChatMember).status
        return status == "creator" || status == "owner"
    }

    fun getMe() = makeJsonRequest<UserResponse>("getMe", null) as TGUser?

    fun getUpdates(offset: Int): Array<TGUpdate>? {
        val values = mapOf(
                "offset" to offset,
                "timeout" to 25
        )
        return makeJsonRequest<UpdatesResponse>("getUpdates", values) as Array<TGUpdate>?
    }

    private fun sendMessage(chatId: Long, text: String): TGMessage? {
        val values = mapOf(
                "chat_id" to chatId,
                "text" to text
        )
        return makeJsonRequest<MessageResponse>("sendMessage", values) as TGMessage?
    }

    private fun sendPhotoURL(chatId: Long, photo: String, caption: String = ""): TGMessage? {
        val values = mapOf(
                "chat_id" to chatId,
                "photo" to photo,
                "caption" to caption
        )
        return makeJsonRequest<MessageResponse>("sendPhoto", values) as TGMessage?
    }

    private fun sendMediaGroupURL(chatId: Long, media: Array<TGInputMedia>): Array<TGMessage>? {
        val values = mapOf(
                "chat_id" to chatId,
                "media" to media
        )
        return makeJsonRequest<MultiMessagesResponse>("sendMediaGroup", values) as Array<TGMessage>?
    }

    @ExperimentalStdlibApi
    fun sendPhotoFile(chatId: Long, photoByteArray: ByteArray, caption: String?): String {
        val values = mapOf(
                "chat_id" to chatId.toString(),
                "caption" to caption
        )
        return makeMultipartRequest(values, photoByteArray)
    }

    fun sendDice(chatId: Long): Int {
        log.info("diceStart")
        val values = mapOf(
                "chat_id" to chatId.toString(),
                "emoji" to "\uD83C\uDFAF"
        )
        val result = makeJsonRequest<MessageResponse>("sendDice", values)
        log.info("converting")
        val message = result as TGMessage?
        log.info("diceEnd")
        return message?.dice?.value ?: -1
    }

    fun sendPoll(
            chatId: Long,
            question: String,
            options: Array<String>,
            answer: Int?,
            closeDate: Long?,
            type: String): String? {
        val values = mapOf(
                "chat_id" to chatId.toString(),
                "question" to question,
                "options" to options,
                "type" to type,
                "correct_option_id" to answer,
                "close_date" to closeDate
        )
        val message = makeJsonRequest<MessageResponse>("sendPoll", values) as TGMessage?
        return message?.poll?.id
    }

    private inline fun <reified T> makeJsonRequest(
            method: String, values: Map<String, Any?>?
    ): Any? {
        log.info("makeJsonRequestStart")
        val requestBody = gson.toJson(values)
        log.info("json of request: $requestBody")

        val request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .uri(URI.create("https://api.telegram.org/bot$token/$method"))
                .header("Content-Type", "application/json")
                .build()

        val json = client.send(request, HttpResponse.BodyHandlers.ofString()).body()
        log.info("json of response: $json")
        log.info("waiting")
        val response = gson.fromJson(json, T::class.java)
        log.info("response: $response")
        return if (response is Response && response.ok) {
            log.info("response.result: $response.result")
            response.result
        }
        else null
    }


    @ExperimentalStdlibApi
    private fun makeMultipartRequest(
            parameters: Map<String, String?>, byteArray: ByteArray
    ): String {
        val multipartBuilder = MultipartEntityBuilder
                .create()
                .addBinaryBody("photo", byteArray)
        for ((key, value) in parameters) {
            multipartBuilder.addTextBody(key, value)
        }
        val multipartData = multipartBuilder.build()
        val requestUploadImage = HttpPost("https://api.telegram.org/bot$token/sendPhoto")
        requestUploadImage.entity = multipartData
        return HttpClientBuilder
                .create()
                .build()
                .execute(requestUploadImage)
                .entity
                .content
                .readAllBytes()
                .decodeToString()
    }
}

abstract class Response {
    abstract val ok: Boolean
    abstract val result: Any
    abstract val description: String?
}

data class UpdatesResponse(
        override val ok: Boolean,
        override val result: Array<TGUpdate>,
        override val description: String?
): Response()

data class MessageResponse(
        override val ok: Boolean,
        override val result: TGMessage,
        override val description: String?
): Response()

data class MultiMessagesResponse(
        override val ok: Boolean,
        override val result: Array<TGMessage>,
        override val description: String?
): Response()

data class UserResponse(
        override val ok: Boolean,
        override val result: TGUser,
        override val description: String?
): Response()

data class ChatMemberResponse(
        override val ok: Boolean,
        override val result: TGChatMember,
        override val description: String?
): Response()

data class TGUpdate(
        val update_id: Int,
        val message: TGMessage?,
        val poll_answer: TGPollAnswer?,
        val poll: TGPoll?
)


data class TGUser(
        val id: Long,
        val is_bot: Boolean,
        val first_name: String?,
        val last_name: String?,
        val username: String?,
        val language_code: String?,
        val can_join_groups: Boolean,
        val can_read_all_group_messages: Boolean,
        val supports_inline_queries: Boolean
)

data class TGChatMember(
        val user: TGUser,
        val status: String
)

data class TGChat(
        val id: Long,
        val type: String,
        val title: String
)

data class TGMessage(
        val message_id: Int,
        val from: TGUser,
        val date: Int,
        val chat: TGChat,
        val text: String?,
        val dice: TGDice?,
        val reply_to_message: TGMessage?,
        val poll: TGPoll?
)

data class TGInputMedia(
        val type: String,
        val media: String
)

data class TGDice(
        val emoji: String,
        val value: Int
)

data class TGPollAnswer(
        val poll_id: String,
        val user: TGUser,
        val option_ids: Array<Int>
)

data class TGPollOption(
        val text: String,
        val voter_count: Int
)

data class TGPoll(
    val id: String,
    val question: String,
    val options: Array<TGPollOption>,
    val total_voter_count: Int,
    val is_closed: Boolean,
    val is_anonymous: Boolean,
    val type: String,
    val allows_multiple_answers: Boolean,
    val correct_option_id: Int?,
    val close_date: Int?
)