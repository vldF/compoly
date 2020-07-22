package api

import com.google.gson.Gson
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.impl.client.HttpClientBuilder
import telApiToken
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.TimeoutException

class TelegramPlatform : PlatformApiInterface {
    private val gson = Gson()
    private val client = HttpClient.newHttpClient()
    private val chatIds = mutableSetOf<Int>()

    override fun send(text: String, chatId: Int, attachments: List<String>) {
        sendMessage(chatId, text)
        chatIds.add(chatId)
    }

    override fun getUserNameById(id: Int): String? {
        for (chatId in chatIds) {
            val values = mapOf(
                    "chat_id" to chatId,
                    "user_id" to id
            )
            val result =
                    makeJsonRequest<ChatMemberResponse>("getChatMember", values)
            if (result != null) return (result as TelegramChatMember).user.username
        }
        return null
    }

    fun getUserIdByName(username: String): Int? {
        for (chatId in chatIds) {
            val values = mapOf(
                    "chat_id" to chatId,
                    "user_id" to "@$username"
            )
            val result =
                    makeJsonRequest<ChatMemberResponse>("getChatMember", values)
            if (result != null) return (result as TelegramChatMember).user.id
        }
        return null
    }

    override fun kickUserFromChat(chatId: Int, userId: Int) {
        val values = mapOf(
                "chat_id" to chatId,
                "user_id" to userId
        )
        makeJsonRequest<Boolean>("kickChatMember", values)
    }

    @ExperimentalStdlibApi
    override fun uploadPhoto(chatId: Int, data: ByteArray): String? {
        return sendPhotoFile(chatId, data, "")
    }

    fun getMe(): TelegramUser? =
            makeJsonRequest<UserResponse>("getMe", null)
                    as TelegramUser?

    fun getUpdates(offset: Int): Array<TelegramUpdate>? {
        val values = mapOf(
                "offset" to offset,
                "timeout" to 25
        )
        return makeJsonRequest<UpdatesResponse>("getUpdates", values)
                as Array<TelegramUpdate>?
    }

    fun sendMessage(chatId: Int, text: String): TelegramMessage? {
        val values = mapOf(
                "chat_id" to chatId,
                "text" to text
        )
        return makeJsonRequest<MessageResponse>("sendMessage", values)
                as TelegramMessage?
    }

    fun sendPhotoURL(chatId: Int, photo: String, caption: String?): TelegramMessage? {
        val values = mapOf(
                "chat_id" to chatId,
                "photo" to photo,
                "caption" to caption
        )
        return makeJsonRequest<MessageResponse>("sendPhoto", values) as TelegramMessage?
    }

    @ExperimentalStdlibApi
    fun sendPhotoFile(chatId: Int, photoByteArray: ByteArray, caption: String?): String {
        val parameters = mapOf(
                "chat_id" to chatId.toString(),
                "caption" to caption
        )
        return makeMultipartRequest<MessageResponse>(parameters, photoByteArray)
    }

    private inline fun <reified T> makeJsonRequest(
            method: String, values: Map<String, Any?>?
    ): Any? {
        val requestBody = gson.toJson(values)

        val request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .uri(URI.create("https://api.telegram.org/bot$telApiToken/$method"))
                .header("Content-Type", "application/json")
                .build()

        val future = try {
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse<String>::body)
        } catch (e: CancellationException) {
            println(e.message)
            return null
        } catch (e: CompletionException) {
            println(e.message)
            return null
        }

        val json = try {
            (future as CompletableFuture<String>).get()
        }
        catch (e: TimeoutException){
            null
        }
        val response = gson.fromJson(json, T::class.java)

        return if (response is Response && response.ok) response.result
        else null
    }


    @ExperimentalStdlibApi
    private inline fun <reified T> makeMultipartRequest(
            parameters: Map<String, String?>, byteArray: ByteArray
    ): String {
        val multipartBuilder = MultipartEntityBuilder
                .create()
                .addBinaryBody("photo", byteArray)
        for ((key, value) in parameters) {
            multipartBuilder.addTextBody(key, value)
        }
        val multipartData = multipartBuilder.build()
        val requestUploadImage = HttpPost("https://api.telegram.org/bot$telApiToken/sendPhoto")
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

open abstract class Response() {
    abstract val ok: Boolean
    abstract val result: Any
    abstract val description: String?
}

data class UpdatesResponse(
        override val ok: Boolean,
        override val result: Array<TelegramUpdate>,
        override val description: String?
): Response()

data class MessageResponse(
        override val ok: Boolean,
        override val result: TelegramMessage,
        override val description: String?
): Response()

data class UserResponse(
        override val ok: Boolean,
        override val result: TelegramUser,
        override val description: String?
): Response()

data class ChatMemberResponse(
        override val ok: Boolean,
        override val result: TelegramChatMember,
        override val description: String?
): Response()


data class TelegramUser(
        val id: Int,
        val is_bot: Boolean,
        val first_name: String?,
        val last_name: String?,
        val username: String?,
        val language_code: String?,
        val can_join_groups: Boolean,
        val can_read_all_group_messages: Boolean,
        val supports_inline_queries: Boolean
)

data class TelegramChatMember(
        val user: TelegramUser,
        val status: String
)

data class TelegramChat(
        val id: Int,
        val type: String,
        val title: String
)

data class TelegramMessage(
        val message_id: Int,
        val from: TelegramUser,
        val date: Int,
        val chat: TelegramChat,
        val text: String
)

data class TelegramUpdate(
        val update_id: Int,
        val message: TelegramMessage
)