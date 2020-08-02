package api

import com.google.gson.Gson
import log
import modules.chatbot.chatModules.Cats
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.impl.client.HttpClientBuilder
import telApiToken
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*

object TelegramPlatform : PlatformApiInterface {
    private val gson = Gson()
    private val client = HttpClient.newHttpClient()
    private val chatIds = setOf<Long>(-445009017)
    private const val token = telApiToken


    override fun send(text: String, chatId: Long, attachments: List<String>) {
        if(attachments.isEmpty()) sendMessage(chatId, text)
        else {
            if (attachments.size == 1) sendPhotoURL(chatId, attachments[0], text)
            else sendMediaGroupURL(
                    chatId,
                    attachments.map { TGInputMedia("photo", it) }.toTypedArray()
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

/*    override fun sendCat(id: Long) {
        send("", id, listOf(catPhotos.poll()))
        catPhotos.add(Cats.getCatUrl())
    }*/

    override fun kickUserFromChat(chatId: Long, userId: Long) {
        val values = mapOf(
                "chat_id" to chatId,
                "user_id" to userId
        )
        makeJsonRequest<Boolean>("kickChatMember", values)
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
        return makeJsonRequest<MessagesResponse>("sendMediaGroup", values) as Array<TGMessage>?
    }

    @ExperimentalStdlibApi
    fun sendPhotoFile(chatId: Int, photoByteArray: ByteArray, caption: String?): String {
        val parameters = mapOf(
                "chat_id" to chatId.toString(),
                "caption" to caption
        )
        return makeMultipartRequest(parameters, photoByteArray)
    }

    private inline fun <reified T> makeJsonRequest(
            method: String, values: Map<String, Any?>?
    ): Any? {
        val requestBody = gson.toJson(values)
        log.info("json of request: $requestBody")

        val request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .uri(URI.create("https://api.telegram.org/bot$token/$method"))
                .header("Content-Type", "application/json")
                .build()

        val json = client.send(request, HttpResponse.BodyHandlers.ofString()).body()
        log.info("json of response: $json")
        val response = gson.fromJson(json, T::class.java)

        return if (response is Response && response.ok) response.result
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

data class MessagesResponse(
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
        val text: String?
)

data class TGUpdate(
        val update_id: Int,
        val message: TGMessage?
)

data class TGInputMedia(
        val type: String,
        val media: String
)