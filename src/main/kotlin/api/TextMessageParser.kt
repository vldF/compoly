package api

import modules.chatbot.chatBotEvents.Platform
import java.util.regex.Pattern
import kotlin.reflect.KClass

class TextMessageParser(private val platform : Platform) {
    private val tgApi = TelegramPlatform
    private val dsApi = DiscordPlatform
    private val mentionRegex = Regex("[a-zA-Z]+(\\d+)\\|(.*)]")

    fun parse(text: String): ParseObject {
        val words = text.split(Pattern.compile("\\s+"))
        val parseObject = ParseObject()

        for ((i, word) in words.withIndex()) {
            when {
                i == 0 && word.startsWith("/") -> {
                    val rawText = words[0].removePrefix("/")
                    val commandText = if(rawText.contains("@")) {
                        rawText.split("@")[0]
                    } else {
                        rawText
                    }

                    val command = Command(commandText, word)
                    parseObject.add(command)
                }

                word.startsWith("@")
                        || word.startsWith("[id")
                        || word.startsWith("[club")
                        || word.startsWith("[public")
                        || word.startsWith("[group")
                        || word.startsWith("<@") ->
                {
                    val processedMention = processMention(word)
                    print(processedMention)
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
            Platform.DISCORD -> { processDiscordMention(text) }
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

    private fun processDiscordMention(text: String): Mention? {
        val mentionRegex = Regex("<@(\\d+)>")
        val regex = mentionRegex.find(text)
        val id = regex?.groupValues?.getOrNull(1)?.removeSurrounding("<@", ">")?.toLongOrNull() ?: return null
        val nick = dsApi.getUserNameById(id) ?: return null
        return Mention(id, nick, text)
    }
}

class ParseObject {
    val data = mutableListOf<AbstractParseData>()

    fun add(newData: AbstractParseData) = data.add(newData)

    operator fun get(index: Int, type: KClass<*>): AbstractParseData? {
        if (!isObjectOnIndexHasType(index, type)) return null
        return data[index]
    }

    // WARNING: Pleas, be cautious; it may throws "can't cast X to T" todo
    inline fun <reified T : AbstractParseData> get(index: Int): T? = data.getOrNull(index) as? T

    fun getTextSlice(start: Int, end: Int) = data.slice(start..end).joinToString(" ") { it.rawText }

    fun isObjectOnIndexHasType(index: Int, type: KClass<*>): Boolean =
        data.getOrNull(index) != null && data.getOrNull(index)!!::class == type

    override fun toString(): String {
        return "ParseObject(data=$data)"
    }

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
    val targetId: Long?,
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

