package api

import database.VirtualMentions
import database.dbQuery
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import kotlin.reflect.KClass

class TextMessageParser {
    companion object {
        private val userMentionRegex = Regex("[a-zA-Z]+(\\d+)\\|(.*)]")
        private val virtualMentionRegex = Regex("@([а-яА-Яa-zA-ZёЁ]+)")
        private val whitespaces = setOf(' ', '\n', '\r')
        private const val COMMAND_START = '/'
        private const val MENTION_START = '['
        private const val MENTION_END = ']'
        private const val VIRTUAL_MENTION_START = '@'
    }

    fun parse(text: String, chatId: Int? = null): ParseObject {
        val buffer = StringBuilder()
        val parseObject = ParseObject()

        var isStart = true
        var state = ParserState.TEXT

        for (inputChar in text) {
            when {
                inputChar == COMMAND_START && isStart -> {
                    state = ParserState.COMMAND
                    buffer.append(inputChar)
                }

                inputChar in whitespaces && state != ParserState.MENTION -> {
                    if (buffer.isBlank()) {
                        buffer.clear()
                        continue
                    }
                    val objectToAdd = parseToken(buffer, state, chatId)
                    parseObject.add(objectToAdd)
                    buffer.clear()

                    state = ParserState.TEXT
                }

                inputChar == MENTION_START && state == ParserState.TEXT -> {
                    if (buffer.isNotBlank()) {
                        val objectToAdd = parseToken(buffer, state, chatId)
                        parseObject.add(objectToAdd)
                        buffer.clear()
                    }

                    state = ParserState.MENTION
                    buffer.append(inputChar)
                }

                inputChar == MENTION_END && state == ParserState.MENTION -> {
                    buffer.append(inputChar)
                    var mention: AbstractParseData? = parseMention(buffer.toString())
                    if (mention == null) {
                        println("error on parsing mention $buffer")
                        mention = Text(buffer.toString())
                    }
                    state = ParserState.TEXT
                    parseObject.add(mention)
                    buffer.clear()
                }

                inputChar == VIRTUAL_MENTION_START && state == ParserState.TEXT -> {
                    buffer.append(inputChar)
                    state = ParserState.VIRTUAL_MENTION
                }

                inputChar.isNumber && (buffer.isEmpty() || state == ParserState.INTEGER) -> {
                    state = ParserState.INTEGER
                    buffer.append(inputChar)
                }

                state == ParserState.INTEGER && !inputChar.isNumber -> {
                    state = ParserState.TEXT
                    buffer.append(inputChar)
                }

                else -> {
                    buffer.append(inputChar)
                }
            }

            isStart = false
        }

        if (buffer.isNotBlank()) {
            val token = parseToken(buffer, state, chatId)
            parseObject.add(token)
        }

        return parseObject
    }

    private fun parseToken(buffer: StringBuilder, state: ParserState, chatId: Int?): AbstractParseData {
        val rawText = buffer.toString()
        return when(state) {
            ParserState.TEXT -> Text(rawText)
            ParserState.COMMAND -> Command(rawText.removePrefix(COMMAND_START.toString()), rawText)
            ParserState.MENTION -> parseMention(rawText) ?: Text(rawText)
            ParserState.INTEGER -> IntegerNumber(rawText.toLong(), rawText)
            ParserState.VIRTUAL_MENTION -> parseVirtualMention(rawText, chatId) ?: Text(rawText)
        }
    }

    private fun parseMention(text: String): Mention? {
        val regex = userMentionRegex.find(text)

        val id = regex?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return null
        val screenName = regex.groupValues.getOrNull(2) ?: return null
        return Mention(id, screenName, false, text)

    }

    private fun parseVirtualMention(text: String, chatId: Int?): Mention? {
        val virtualRegex = virtualMentionRegex.find(text)
        val name = virtualRegex?.groupValues?.get(1) ?: return null
        val targetId = if (chatId != null) {
            getVirtualUserId(chatId, name)
        } else null
        return Mention(targetId, name, true, text)
    }

    private fun getVirtualUserId(chatId: Int, name: String): Int? {
        return dbQuery {
            VirtualMentions.select {
                (VirtualMentions.chatId eq chatId) and (VirtualMentions.name eq name)
            }.firstOrNull()
        }?.getOrNull(VirtualMentions.id)
    }

    private val Char.isNumber: Boolean
        get() = this in '0'..'9' || this == '-'
}

class ParseObject {
    val data = mutableListOf<AbstractParseData>()

    fun add(newData: AbstractParseData) = data.add(newData)

    operator fun get(index: Int, type: KClass<*>): AbstractParseData? {
        if (!isObjectOnIndexHasType(index, type)) return null
        return data[index]
    }

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

private enum class ParserState {
    TEXT, COMMAND, MENTION, VIRTUAL_MENTION, INTEGER
}


abstract class AbstractParseData {
    abstract val rawText: String
}

data class Command(
    val name: String,
    override val rawText: String
) : AbstractParseData()

data class Mention(
    val targetId: Int?,
    val targetScreenName: String,
    val isVirtual: Boolean,
    override val rawText: String
) : AbstractParseData()

data class Text(
    override val rawText: String
) : AbstractParseData()

data class IntegerNumber(
    val number: Long,
    override val rawText: String
) : AbstractParseData()

