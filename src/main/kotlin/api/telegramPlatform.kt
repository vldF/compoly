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
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.TimeoutException

class TelegramPlatform : PlatformApiInterface {
    val gson = Gson()

    override fun send(text: String, chatId: Int, attachments: List<String>) {
        TODO("Not yet implemented")
    }

    override fun getUserNameById(id: Int): String? {
        TODO("Not yet implemented")
    }

    override fun kickUserFromChat(chatId: Int, userId: Int) {
        TODO("Not yet implemented")
    }

    override fun uploadPhoto(chatId: Int, data: ByteArray): String? {
        TODO("Not yet implemented")
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

    fun sendPhotoFile(chatId: Int, photoPath: String, caption: String?) {
        val fileBody = FileBody(File(photoPath))
        val parameters = mapOf(
                "chat_id" to chatId.toString(),
                "caption" to caption
        )
        makeMultipartRequest(parameters,fileBody)
    }

    private inline fun <reified T> makeJsonRequest(
            method: String, values: Map<String, Any?>?
    ): Any? {
        val requestBody = gson.toJson(values)

        val client = HttpClient.newHttpClient()
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
        val result =
                if (response is Response) response?.result
                else null
        return result
    }


    private fun makeMultipartRequest(parameters: Map<String, String?>, fileBody: FileBody) {
        val multipartBuilder = MultipartEntityBuilder
                .create()
                .addPart("photo", fileBody)
        for ((key, value) in parameters) {
            multipartBuilder.addTextBody(key, value)
        }
        val multipartData = multipartBuilder.build()
        val requestUploadImage = HttpPost("https://api.telegram.org/bot$telApiToken/sendPhoto")
        requestUploadImage.entity = multipartData
        HttpClientBuilder
                .create()
                .build()
                .execute(requestUploadImage)
    }
}

open abstract class Response(
        ok: Boolean,
        description: String?
) {
    abstract val result: Any
}

data class UpdatesResponse(
        val ok: Boolean,
        override val result: Array<TelegramUpdate>,
        val description: String?
): Response(ok, description)

data class MessageResponse(
        val ok: Boolean,
        override val result: TelegramMessage,
        val description: String?
): Response(ok, description)

data class UserResponse(
        val ok: Boolean,
        override val result: TelegramUser,
        val description: String?
): Response(ok, description)


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

data class TelegramChat(
        val id: Int,
        val type: String,
        val title: String
)

data class TelegramMessage(
        val message_id: Int,
        val from: TelegramUser,
        val date: Integer,
        val chat: TelegramChat,
        val text: String
)

data class TelegramUpdate(
        val update_id: Int,
        val message: TelegramMessage
)