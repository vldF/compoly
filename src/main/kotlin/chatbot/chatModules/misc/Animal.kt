package chatbot.chatModules.misc

import api.VkPlatform
import chatbot.chatBotEvents.LongPollNewMessageEvent
import mainChatPeerId
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.LinkedBlockingQueue

/**Class for loading pictures of live animals (cats, dogs, etc.) from the link [animalApiLink]*/
abstract class Animal {
    /**Link to animal api*/
    abstract val animalApiLink: String

    /**Http Client*/
    private val client: HttpClient = HttpClient.newHttpClient()

    /**Max size of animals images cache*/
    private val vkPixQueueSize = 4 // todo: change it to 4 in prod; 1 was set for faster loading

    /**Animals images cache*/
    private val vkAnimalQueue = LinkedBlockingQueue<String>(vkPixQueueSize)

    /**Use this fun with annotation OnCommand (see [chatbot/Annotations.kt])*/
    fun animal(event: LongPollNewMessageEvent) {
        val api = event.api
        if (vkAnimalQueue.isEmpty()) addAnimalsToQueue(vkPixQueueSize, api, event.chatId)
        val catAttachment = vkAnimalQueue.poll()
        api.sendWithAttachments("", event.chatId, listOf(catAttachment))
        addAnimalsToQueue(api = api, chatId = event.chatId)
    }

    /**Connect to animal api and get Json with image link. Parse it via [parseAnimalResponse]*/
    private fun getAnimalUrl(): String {
        val requestJson = HttpRequest.newBuilder()
            .uri(URI.create(animalApiLink))
            .timeout(Duration.ofSeconds(10))
            .build()
        val response = client.send(
            requestJson,
            HttpResponse.BodyHandlers.ofString()
        )
        return parseAnimalResponse(response.body())
    }

    /**Parse the response from animal api*/
    protected abstract fun parseAnimalResponse(response: String): String

    /**
     * Uploading [count] pictures of an animal (max [vkPixQueueSize]) to the [chatId]
     * and adding them to the [vkAnimalQueue]*/
    private fun addAnimalsToQueue(count: Int = 1, api: VkPlatform, chatId: Int = mainChatPeerId) {
        for (i in 0 until count) {
            val url = getAnimalUrl()
            val attachment = api.uploadPhotoByUrlAsAttachment(chatId, url) ?: continue
            vkAnimalQueue.add(attachment)
        }
    }
}