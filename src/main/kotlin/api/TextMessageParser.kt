package api

import kotlin.reflect.KClass

abstract class TextMessageParser {
    fun parse(text: String): ParseObject {
        val words = text.split(" ")
        val parseObject = ParseObject()

        for ((i, word) in words.withIndex()) {
            when {
                i == 0 && word.startsWith("/") -> {
                    val command = Command(words[0].removePrefix("/"), word)
                    parseObject.add(command)
                }

                word.startsWith("@") -> {
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

    abstract fun processMention(text: String) : Mention?
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

class ParseObject {
    private val data = mutableListOf<AbstractParseData>()

    fun add(newData: AbstractParseData) = data.add(newData)

    fun get(index: Int, type: KClass<*>): AbstractParseData? {
        if (!isObjectOnIndexHasType(index, type)) return null
        return data[index]
    }

    fun getTextSlice(start: Int, end: Int) = data.slice(start..end).joinToString(" ") { it.rawText }

    fun isObjectOnIndexHasType(index: Int, type: KClass<*>): Boolean =
        data.getOrNull(index) != null && data.getOrNull(index)!!::class == type
}