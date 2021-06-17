package chatbot.chatModules

import chatbot.ModuleObject
import chatbot.OnCommand
import chatbot.UsageInfo
import chatbot.chatBotEvents.LongPollNewMessageEvent
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlin.random.Random

@ModuleObject
object Joke {
    private const val notEnoughMessage = "Товарищ, шутки кончились. Придумывание новых анекдотов происходит раз в 4 часа"
    private val client = HttpClient.newHttpClient()

    @UsageInfo(baseUsageAmount = 2, levelBonus = 1, notEnoughMessage)
    @OnCommand(["шутка", "анекдот", "анек", "joke", "anecdote"], "Смешной анекдот категории Б")
    fun joke(event: LongPollNewMessageEvent) {
        val api = event.api

        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://baneks.ru/${Random.nextInt(1143)}"))
            .timeout(Duration.ofSeconds(10))
            .build()

        val response = client.send(
            request,
            HttpResponse.BodyHandlers.ofString()
        ).body()

        api.send(parseResponse(response), event.chatId)
    }

    private fun parseResponse(response: String): String {
        return response.substringAfter("<p>").substringBefore("</p>").replace("<br />", "")
    }
}