package chatbot.chatModules

import chatbot.ModuleObject
import chatbot.OnCommand
import chatbot.UsageInfo
import chatbot.chatBotEvents.LongPollNewMessageEvent
import com.google.gson.Gson
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlin.random.Random

@ModuleObject
object Joke {
    private const val notEnoughMessage = "Товарищ, шутки кончились. Придумывание новых анекдотов происходит раз в 4 часа"

    @UsageInfo(baseUsageAmount = 2, levelBonus = 1, notEnoughMessage)
    @OnCommand(["шутка", "анекдот", "анек", "joke", "anecdote"], "Смешной анекдот")
    fun story(event: LongPollNewMessageEvent) {
        createAndSendContent(RzhunemoguContentType.JOKE, event)
    }
}

@ModuleObject
object Story {
    private const val notEnoughMessage = "Товарищ, рассказы кончились. Придумывание новых историй происходит раз в 4 часа"

    @UsageInfo(baseUsageAmount = 2, levelBonus = 1, notEnoughMessage)
    @OnCommand(["рассказ", "история", "story"], "Забавная история")
    fun joke(event: LongPollNewMessageEvent) {
        createAndSendContent(RzhunemoguContentType.STORY, event)
    }
}

@ModuleObject
object Verse {
    private const val notEnoughMessage = "Товарищ, стишки кончились. Придумывание новых стишков происходит раз в 4 часа"

    @UsageInfo(baseUsageAmount = 2, levelBonus = 1, notEnoughMessage)
    @OnCommand(["стишок", "стих", "verse"], "Необычный стишок")
    fun verse(event: LongPollNewMessageEvent) {
        createAndSendContent(RzhunemoguContentType.VERSE, event)
    }
}

@ModuleObject
object Aphorism {
    private const val notEnoughMessage = "Товарищ, афоризмы кончились. Придумывание новых афоризмов происходит раз в 4 часа"

    @UsageInfo(baseUsageAmount = 2, levelBonus = 1, notEnoughMessage)
    @OnCommand(["афоризм", "aphorism"], "Глубокий афоризм")
    fun aphorism(event: LongPollNewMessageEvent) {
        createAndSendContent(RzhunemoguContentType.APHORISM, event)
    }
}

@ModuleObject
object Quote {
    private const val notEnoughMessage = "Товарищ, цитаты кончились. Придумывание новых цитат происходит раз в 4 часа"

    @UsageInfo(baseUsageAmount = 2, levelBonus = 1, notEnoughMessage)
    @OnCommand(["цитата", "quote"], "Интересная цитата")
    fun aphorism(event: LongPollNewMessageEvent) {
        createAndSendContent(RzhunemoguContentType.QUOTE, event)
    }
}

@ModuleObject
object Toast {
    private const val notEnoughMessage = "Товарищ, тосты кончились. Придумывание новых тостов происходит раз в 4 часа"

    @UsageInfo(baseUsageAmount = 2, levelBonus = 1, notEnoughMessage)
    @OnCommand(["тост", "toast"], "Поучительный тост")
    fun aphorism(event: LongPollNewMessageEvent) {
        createAndSendContent(RzhunemoguContentType.TOAST, event)
    }
}

@ModuleObject
object Status {
    private const val notEnoughMessage = "Товарищ, статусы кончились. Придумывание новых статусов происходит раз в 4 часа"

    @UsageInfo(baseUsageAmount = 2, levelBonus = 1, notEnoughMessage)
    @OnCommand(["статус", "status"], "Запоминающийся статус")
    fun aphorism(event: LongPollNewMessageEvent) {
        createAndSendContent(RzhunemoguContentType.STATUS, event)
    }
}

private const val congratulationsOnVulgarity = "Наши поздравления, товарищ! Вам выпала пошлая версия\uD83D\uDE0F:\n\n"

enum class RzhunemoguContentType(val num: Int) {
    JOKE(1),
    STORY(2),
    VERSE(3),
    APHORISM(4),
    QUOTE(5),
    TOAST(6),
    STATUS(8)
}

private val client = HttpClient.newHttpClient()
val gson = Gson()

private fun getResponseFromRzhunemogu(type: Int): String {
    val request = HttpRequest.newBuilder()
        .uri(URI.create("http://rzhunemogu.ru/RandJSON.aspx?CType=$type"))
        .timeout(Duration.ofSeconds(10))
        .build()

    return client.send(
        request,
        HttpResponse.BodyHandlers.ofString()
    ).body()
}

private fun calculateVulgarProbability(): Boolean {
    return Random.nextInt(6) == 0
}

private fun createAndSendContent(contentType: RzhunemoguContentType, event: LongPollNewMessageEvent) {
    fun String.parse(): String? = gson.fromJson(this, JokeResponse::class.java)?.content

    val isVulgar = calculateVulgarProbability()
    var typeNum = contentType.num
    if (isVulgar)
        typeNum += 10

    val response = buildString {
        val content = getResponseFromRzhunemogu(typeNum).parse() ?: "Ошибка при получении :("
        if (isVulgar)
            append(congratulationsOnVulgarity)

        append(content)
    }

    event.api.send(response, event.chatId)
}

data class JokeResponse(
    val content: String
)
