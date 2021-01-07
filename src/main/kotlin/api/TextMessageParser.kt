package api

import database.VirtualMentions
import database.dbQuery
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import java.util.regex.Pattern
import kotlin.reflect.KClass

class TextMessageParser {
    companion object {
        private val userMentionRegex = Regex("[a-zA-Z]+(\\d+)\\|(.*)]")
        private val virtualMentionRegex = Regex("@([а-яА-Яa-zA-ZёЁ]+)")
        private val commandRegex = Regex("^\\/([a-zA-Zа-яА-ЯёЁ_1-9]+)")
    }

    fun parse(text: String, chatId: Int? = null): ParseObject {
        val words = text.split(Pattern.compile("\\s+"))
        val parseObject = ParseObject()

        loop@ for ((i, word) in words.withIndex()) {
            when {
                i == 0 && word.startsWith("/") -> {
                    val commandText = commandRegex.find(words[0])?.groupValues?.get(1) ?: continue@loop
                    val command = Command(commandText, word)
                    parseObject.add(command)
                }

                word.startsWith("@")
                        || word.startsWith("[id")
                        || word.startsWith("[club")
                        || word.startsWith("[public")
                        || word.startsWith("[group") ->
                {
                    val processedMention = processMention(word, chatId)
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

    private fun processMention(text: String, chatId: Int?): Mention? {
        val regex = userMentionRegex.find(text)

        if (regex != null) {
            // this is mention of real user
            val id = regex.groupValues.getOrNull(1)?.toIntOrNull() ?: return null
            val screenName = regex.groupValues.getOrNull(2) ?: return null
            return Mention(id, screenName, false, text)
        } else {
            // this is virtual mention. Or isn't
            val virtualRegex = virtualMentionRegex.find(text)
            val name = virtualRegex?.groupValues?.get(1) ?: return null
            val targetId = if (chatId != null) {
                getVirtualUserId(chatId, name)
            } else null
            return Mention(targetId, name, true, text)
        }
    }

    private fun getVirtualUserId(chatId: Int, name: String): Int? {
        return dbQuery {
            VirtualMentions.select {
                (VirtualMentions.chatId eq chatId) and (VirtualMentions.name eq name)
            }.firstOrNull()
        }?.getOrNull(VirtualMentions.id)
    }
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

