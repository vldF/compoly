package api

import modules.chatbot.chatBotEvents.Platform
import telTestToken
import kotlin.reflect.KClass

class TextMessageParser(private val platform : Platform) {
    private val tgApi = TelegramPlatform(telTestToken)

    private val mentionRegex = Regex("id(\\d+)\\|(.*)]")

    fun parse(text: String): ParseObject {
        val words = text.split(" ")
        val parseObject = ParseObject()

        for ((i, word) in words.withIndex()) {
            when {
                i == 0 && word.startsWith("/") -> {
                    val command = Command(words[0].removePrefix("/"), word)
                    parseObject.add(command)
                }

                word.startsWith("@") || word.startsWith("[id") -> {
                    val processedMention = processMention(word)
                    if (processedMention == null) {
                        parseObject.add(Text(word))
                    } else {
                        parseObject.add(processedMention)
                    }
                }

                word.toLongOrNull() != null -> {
                    parseObject.add(IntegerNumber(word.toLong(), word))
                }

                else -> {
                    parseObject.add(Text(word))
                }
            }
        }

        return parseObject
    }

    private fun processMention(text: String) : Mention? {
        return when (platform) {
            Platform.VK -> { processVkMention(text) }
            Platform.TELEGRAM -> { processTelegramMention(text) }
            Platform.DISCORD -> { null }
        }
    }

    private fun processTelegramMention(text: String): Mention? {
        val nick = text.removePrefix("@")
        val id = tgApi.getUserIdByName(nick) ?: return null

        return Mention(id, nick, text)
    }

    private fun processVkMention(text: String): Mention? {
        val regex = mentionRegex.find(text)

        val id = regex?.groupValues?.getOrNull(1)?.toLongOrNull() ?: return null
        val screenName = regex.groupValues.getOrNull(2) ?: return null

        return Mention(id, screenName, text)
    }
}

class ParseObject {
    private val data = mutableListOf<AbstractParseData>()

    fun add(newData: AbstractParseData) = data.add(newData)

    operator fun get(index: Int, type: KClass<*>): AbstractParseData? {
        if (!isObjectOnIndexHasType(index, type)) return null
        return data[index]
    }

    fun <T : AbstractParseData> get(index: Int): T? = data.getOrNull(index) as? T

    fun getTextSlice(start: Int, end: Int) = data.slice(start..end).joinToString(" ") { it.rawText }

    fun isObjectOnIndexHasType(index: Int, type: KClass<*>): Boolean =
        data.getOrNull(index) != null && data.getOrNull(index)!!::class == type

    val size: Int
        get() = data.size
}


abstract class AbstractParseData {
    abstract val rawText: String
}

data class Command(
    val name: String,
    override val rawText: String
) : AbstractParseData()

data class Mention(
    val targetId: Long,
    val targetScreenName: String,
    override val rawText: String
) : AbstractParseData()

data class Text(
    override val rawText: String
) : AbstractParseData()

data class IntegerNumber(
    val number: Long,
    override val rawText: String
) : AbstractParseData()

