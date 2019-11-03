package api

import vkApiToken
import java.lang.StringBuilder
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.time.Duration
import kotlin.random.Random
import java.net.http.HttpResponse


class Vk (private val token: String) {
    fun post(methodName: String, params: MutableMap<String, String>) {
        val reqParams = StringBuilder()
        reqParams.append("access_token=$token&")
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
        println(response.body())
    }

    fun send(message: String, chatId: String) {
        this.post("messages.send", mutableMapOf(
            "message" to message,
            "chat_id" to chatId
        ))
    }
}

fun main() {
    Vk(vkApiToken).post(
        "messages.send",
        mutableMapOf(
            "message" to "test",
            "chat_id" to "1"
            )
        )
}