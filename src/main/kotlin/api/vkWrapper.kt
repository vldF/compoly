package api

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
import java.lang.StringBuilder
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlin.random.Random


class Vk {
    private val client = HttpClientBuilder.create().build()

    @Suppress("SameParameterValue")
    @OptIn(ExperimentalStdlibApi::class)
    fun post(methodName: String, params: MutableMap<String, Any>): String? {
        if (testMode && methodName == "messages.send") {
            return null
        }

        /*val reqParams = mutableListOf<BasicNameValuePair>()
        reqParams.add(BasicNameValuePair("access_token", vkApiToken))
        reqParams.add(BasicNameValuePair("v", "5.103"))
        reqParams.add(BasicNameValuePair("random_id", System.currentTimeMillis().toString())) // todo
        for ((p, v) in params) {
            reqParams.add(BasicNameValuePair(p, v.toString()))
        }
        log.info("reqParams: $reqParams")

        val request = HttpPost("https://api.vk.com/method/$methodName")
        request.entity = UrlEncodedFormEntity(reqParams)
        log.info("requst: $request")
        log.info("requst.entity: ${request.entity}")
        log.info("requst.entity.content: ${request.entity.content}")
        val response = client.execute(request).entity.content.readAllBytes()?.decodeToString()
        log.info("response: $response")
        return response*/

        val reqParams = StringBuilder()
        reqParams.append("access_token=$vkApiToken&")
        reqParams.append("v=5.103&")
        reqParams.append("random_id=${Random(System.currentTimeMillis()).nextInt()}&")
        for ((p, v) in params) {
            reqParams.append("$p=$v&")
        }
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.vk.com/method/$methodName"))
            .timeout(Duration.ofSeconds(10))
            .POST(HttpRequest.BodyPublishers.ofString(reqParams.toString()))
            .build()
        val client = HttpClient.newHttpClient()
        val response = client.send(
            request,
            HttpResponse.BodyHandlers.ofString()
        )
        log.info("response: ${response.body()}")
        return response.body()
    }

    fun send(text: String, chatId: List<Int>, attachments: List<String> = listOf()) {
        val message = Message(text, chatId, attachments)
        SendMessageThread.addInList(message)
    }

    fun getConversationMembersByPeerID(peer_id: String, fields: List<String>) =
        post(
            "messages.getConversationMembers",
            mutableMapOf(
                "peer_id" to peer_id,
                "fields" to fields.joinToString(separator = ",")
            )
        )

    @OptIn(ExperimentalStdlibApi::class)
    fun uploadImage(peer_id: Int, data: ByteArray): String {
        val serverData = post(
            "photos.getMessagesUploadServer",
            mutableMapOf(
                    "peer_id" to peer_id
            )
        )

        val jsonServer = JsonParser().parse(serverData).asJsonObject
        val vkResponse = jsonServer["response"].asJsonObject ?: throw IllegalStateException(serverData)
        val uploadUrl = vkResponse["upload_url"].asString

        val inputStreamBody = InputStreamBody(ByteArrayInputStream(data), "compolylovesiknt.jpg")

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
        )

        val dataObject = JsonParser().parse(saveData)
            .asJsonObject["response"]
            .asJsonArray[0]
            .asJsonObject

        val ownerId = dataObject["owner_id"]
        val imageId = dataObject["id"]

        return "photo${ownerId}_$imageId"
    }

}
