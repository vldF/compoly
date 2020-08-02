package api

import api.objects.VkUser
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import log
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.InputStreamBody
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.message.BasicNameValuePair
import testMode
import vkApiToken
import java.io.ByteArrayInputStream
import java.net.URL
import java.util.*


class VkPlatform : PlatformApiInterface {
    private val client = HttpClientBuilder.create().build()
    private val gson = Gson()
    val catPhotos: Queue<String> = LinkedList()

    fun getChatMembers(peer_id: Long, fields: List<String>): List<VkUser>? {
        val resp = post(
                "messages.getConversationMembers",
                mutableMapOf(
                        "peer_id" to peer_id,
                        "fields" to fields.joinToString(separator = ",")
                )
        ) ?: return null

        val vkResponse = resp["response"]?.asJsonObject ?: return null
        val profiles = vkResponse["items"].asJsonArray

        return profiles.map { gson.fromJson(it, VkUser::class.java) }
    }

    override fun getUserIdByName(showingName: String): Long? {
        val resp = post(
                "users.get", mutableMapOf(
                "user_ids" to showingName
            )
        )
        val json = resp?.asJsonObject
        return json?.get("response")?.asJsonArray?.get(0)?.asJsonObject?.get("id")?.asLong
    }

    override fun getUserNameById(id: Long): String? {
        val resp = post("users.get", mutableMapOf(
                "user_ids" to id,
                "fields" to "screen_name"
        ))
        val json = resp?.asJsonObject
        return json?.get("response")?.asJsonArray?.get(0)?.asJsonObject?.get("screen_name")?.asString
    }

    override fun kickUserFromChat(chatId: Long, userId: Long) {
        post("messages.removeChatUser", mutableMapOf(
                "chat_id" to chatId - 2000000000,
                "user_id" to userId
        ))
    }

    fun convertUrlToVkPhoto(chatId: Long?, url: String): String? {
        val imageConnection = URL(url).openConnection()
        val imageStream = imageConnection.getInputStream()
        return uploadPhoto(chatId, imageStream.readBytes())
    }

    override fun send(text: String, chatId: Long, urls: List<String>) {
        val attachments = mutableListOf<String>()
        for (url in urls) attachments.add(convertUrlToVkPhoto(chatId, url) ?: "")
        sendPhotos(text, chatId, attachments)
    }

    fun sendPhotos(text: String, chatId: Long, attachments: List<String>) {
        val message = Message(text, listOf(chatId), attachments)
        SendMessageThread.addInList(message)
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun uploadPhoto(peer_id: Long?, data: ByteArray): String? {
        val serverData =
                if (peer_id != null) post(
                    "photos.getMessagesUploadServer",
                    mutableMapOf("peer_id" to peer_id)
                )
                else post("photos.getMessagesUploadServer", mutableMapOf())


        val jsonServer = serverData?.asJsonObject ?: return null
        val vkResponse = jsonServer["response"].asJsonObject ?: throw IllegalStateException(serverData.asString)
        val uploadUrl = vkResponse["upload_url"].asString

        val inputStreamBody = InputStreamBody(ByteArrayInputStream(data), "compoly_loves_iknt.jpg")

        val multipartData = MultipartEntityBuilder
            .create()
            .addPart("photo", inputStreamBody)
            .build()

        val requestUploadImage = HttpPost(uploadUrl)
        requestUploadImage.entity = multipartData

        @ExperimentalStdlibApi
        val responseUpload = HttpClientBuilder
            .create()
            .build()
            .execute(requestUploadImage)
            .entity
            .content
            .readAllBytes()
            .decodeToString()

        val jsonUpload = JsonParser().parse(responseUpload).asJsonObject
        val server = jsonUpload["server"].asInt
        val photo = jsonUpload["photo"].asString
        val hash = jsonUpload["hash"].asString

        val saveData = post(
            "photos.saveMessagesPhoto",
            mutableMapOf(
                "server" to server,
                "photo" to photo,
                "hash" to hash
            )
        ) ?: return null

        val dataObject = saveData
            .asJsonObject["response"]
            .asJsonArray[0]
            .asJsonObject

        val ownerId = dataObject["owner_id"]
        val imageId = dataObject["id"]

        return "photo${ownerId}_$imageId"
    }


    @Suppress("SameParameterValue")
    @OptIn(ExperimentalStdlibApi::class)
    fun post(methodName: String, params: MutableMap<String, Any>): JsonObject? {
        if (testMode && methodName == "messages.send") {
            return null
        }

        val reqParams = mutableListOf<BasicNameValuePair>()
        reqParams.add(BasicNameValuePair("access_token", vkApiToken))
        reqParams.add(BasicNameValuePair("v", "5.103"))
        for ((p, v) in params) {
            reqParams.add(BasicNameValuePair(p, v.toString()))
        }

        val request = HttpPost("https://api.vk.com/method/$methodName")
        request.entity = UrlEncodedFormEntity(reqParams, charset("utf-8"))
        val response = client.execute(request).entity.content.readAllBytes()
                ?.decodeToString()
        log.info("response: $response")
        request.releaseConnection()
        return JsonParser().parse(response).asJsonObject
    }
}