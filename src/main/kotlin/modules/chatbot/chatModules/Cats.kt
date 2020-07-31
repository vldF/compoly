package modules.chatbot.chatModules

import api.DiscordPlatform.uploadPhoto
import api.VkPlatform
import com.google.gson.JsonParser
import modules.Active
import modules.chatbot.OnCommand
import modules.chatbot.chatBotEvents.LongPollEventBase
import java.net.URI
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Active
object Cats {
    private val theCatApiKey = "dc64b39c-51b6-43aa-ba44-a231e8937d5b"
    private val client = HttpClient.newHttpClient()

    fun getCatUrl(): String {
        val requestJson = HttpRequest.newBuilder()
                .uri(URI.create("https://api.thecatapi.com/v1/images/search?api_key=$theCatApiKey"))
                .timeout(Duration.ofSeconds(10))
                .build()
        val response = client.send(
                requestJson,
                HttpResponse.BodyHandlers.ofString()
        )
        val catInfo = JsonParser().parse(response.body()).asJsonArray[0].asJsonObject
        return catInfo["url"].asString
    }

    @OnCommand(["котик", "cat"], "КОТИКИ!", cost = 20)
    fun cat(event: LongPollEventBase) {
        val url = getCatUrl()
        val api = event.api
        if (api is VkPlatform) {
            if (api.catPhotos.isEmpty())
                api.catPhotos.add(api.convertUrlToVkPhoto(null, getCatUrl()))
            api.sendPhotos("", event.chatId, listOf(api.catPhotos.poll()))
            api.catPhotos.add(api.convertUrlToVkPhoto(null, url))
        }
        else api.send("", event.chatId, listOf(url))
    }
}

