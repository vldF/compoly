package modules.chatbot.chatModules

import api.VkPlatform
import com.google.gson.JsonParser
import modules.Active
import modules.chatbot.MessageNewObj
import modules.chatbot.OnCommand
import java.net.URI
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Active
class Cats {
    private val theCatApiKey = "dc64b39c-51b6-43aa-ba44-a231e8937d5b"
    private val client = HttpClient.newHttpClient()
    private val vk = VkPlatform()

    @OnCommand(["котик", "cat"], "КОТИКИ!", cost = 20)
    fun cat(messageObj: MessageNewObj) {
        val requestJson = HttpRequest.newBuilder()
                .uri(URI.create("https://api.thecatapi.com/v1/images/search?api_key=$theCatApiKey"))
                .timeout(Duration.ofSeconds(10))
                .build()
        val response = client.send(
                requestJson,
                HttpResponse.BodyHandlers.ofString()
        )

        val catInfo = JsonParser().parse(response.body()).asJsonArray[0].asJsonObject
        val imageUrl = catInfo["url"].asString

        val imageConnection = URL(imageUrl).openConnection()
        val imageStream = imageConnection.getInputStream()

        val attachment = vk.uploadImage(messageObj.peer_id, imageStream.readBytes()) ?: ""
        vk.send("", messageObj.peer_id, listOf(attachment))
    }
}

