package modules.chatbot.chatModules

import api.VkPlatform
import com.google.gson.JsonParser
import modules.chatbot.ModuleObject
import modules.chatbot.OnCommand
import modules.chatbot.chatBotEvents.LongPollEventBase
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.LinkedBlockingQueue

@ExperimentalStdlibApi
@ModuleObject
object Cats {
    private val theCatApiKey = "dc64b39c-51b6-43aa-ba44-a231e8937d5b"
    private val client = HttpClient.newHttpClient()
    private const val VK_PIX_QUEUE_SIZE = 4
    private val vkCatsQueue = LinkedBlockingQueue<String>(VK_PIX_QUEUE_SIZE)

    init {
        addCatsToQueue(VK_PIX_QUEUE_SIZE)
    }

    @OnCommand(["котик", "cat"], "КОТИКИ!", cost = 20)
    fun cat(event: LongPollEventBase) {
        val api = event.api
        if (api is VkPlatform) {
            val catAttachment = vkCatsQueue.poll()
            api.sendPhotos("", event.chatId, listOf(catAttachment))
            addCatsToQueue(1)
        } else {
            val url = getCatUrl()
            api.send("", event.chatId, listOf(url))
        }
    }

    private fun getCatUrl(): String {
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

    private fun addCatsToQueue(count: Int = 1) {
        val vkApi = VkPlatform()
        for (i in 0 until count) {
            val url = getCatUrl()
            val attachment = vkApi.uploadPhotoByUrlAsAttachment(null, url) ?: continue
            vkCatsQueue.add(attachment)
        }
    }
}

