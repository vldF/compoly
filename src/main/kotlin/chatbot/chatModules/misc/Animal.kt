package chatbot.chatModules.misc

import api.VkApi
import chatbot.chatBotEvents.LongPollNewMessageEvent
import configs.mainChatPeerId
import krobot.api.`try`
import log
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.*


/** Class for loading pictures of live animals (cats, dogs, etc.) from the link [animalApiLink] */
abstract class Animal {
    /** Link to animal api */
    abstract val animalApiLink: String

    /** Http Client */
    private val client: HttpClient = HttpClient.newHttpClient()

    /** Max size of animals images cache */
    private val vkPixQueueSize = 4 // todo: change it to 4 in prod; 1 was set for faster loading

    /** Animals images cache */
    private val vkAnimalQueue = LinkedList<String>()

    /** In case if no image available */
    private val noImage = "https://sun9-22.userapi.com/impg/9DSAvuiYG8-a8ZoTULK0c7qXa-Ze5EZD8jU0YA/-FzHoXGxfQM.jpg?" +
            "size=257x307&quality=96&sign=a9f6943997073aa917da6350453f2c3c&type=album"

    /** Use this fun with annotation OnCommand (see [chatbot/Annotations.kt]) */
    fun animal(event: LongPollNewMessageEvent) {
        val api = event.api
        if (vkAnimalQueue.isEmpty()) addAnimalsToQueue(vkPixQueueSize, api, event.chatId)
        val catAttachment = vkAnimalQueue.poll()
        api.sendWithAttachments("", event.chatId, listOf(catAttachment))
        addAnimalsToQueue(api = api, chatId = event.chatId)
    }

    /** Connect to animal api and get Json with image link. Parse it via [parseAnimalResponse] */
    private fun getAnimalUrl(): String {
        return try {
            val requestJson =
                HttpRequest.newBuilder()
                    .uri(URI.create(animalApiLink))
                    .timeout(Duration.ofSeconds(10))
                    .build()
            val response = client.send(
                requestJson,
                HttpResponse.BodyHandlers.ofString()
            )
            parseAnimalResponse(response.body())
        } catch (e: IOException) {
            log.severe("error on getting picture")
            log.severe(e.stackTraceToString())
            noImage
        }
    }

    /** Parse the response from animal api */
    protected abstract fun parseAnimalResponse(response: String): String

    /**
     * Uploading [count] pictures of an animal (max [vkPixQueueSize]) to the [chatId]
     * and adding them to the [vkAnimalQueue]
     * */
    private fun addAnimalsToQueue(count: Int = 1, api: VkApi, chatId: Int = mainChatPeerId) {
        for (i in 0 until count) {
            val url = getAnimalUrl()
            val attachment = api.uploadPhotoByUrlAsAttachment(url) ?: continue
            vkAnimalQueue.add(attachment)
        }
    }
}