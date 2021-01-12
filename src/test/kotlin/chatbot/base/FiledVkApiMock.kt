package chatbot.base

import api.keyboards.Keyboard
import api.objects.VkUser
import com.google.gson.Gson
import java.io.File

class FiledVkApiMock(
    private val pathToFile: String,
    private val keeper: ApiResponseKeeper
) : VkApiMock {
    override val meId: Long = -1

    private val valuesReadCount = mutableMapOf<String, Int>()

    override fun getUserIdByName(username: String): Long? {
        writeResponse("getUserIdByName", "username" to username)
        return readValueFromFile(pathToFile, "getUserIdByName", 1L)
    }

    override fun getUserNameById(id: Long): String? {
        writeResponse("getUserNameById", "id" to id)
        return readValueFromFile(pathToFile, "getUserNameById", "Test User")
    }

    override fun kickUserFromChat(chatId: Long, userId: Long) {
        writeResponse("kickUserFromChat", "chatId" to chatId, "userId" to userId)
    }

    override fun isUserAdmin(chatId: Long, userId: Long): Boolean {
        writeResponse("isUserAdmin", "chatId" to chatId, "userId" to userId)
        return readValueFromFile(pathToFile, "isUserAdmin", false)
    }

    override fun uploadPhotoByUrlAsAttachment(chatId: Long?, url: String): String? {
        writeResponse("uploadPhotoByUrlAsAttachment", "chatId" to chatId, "url" to url)
        return readValueFromFile(pathToFile, "isUserAdmin", "empty_url")
    }

    override fun send(text: String, chatId: Long, pixUrls: List<String>, keyboard: Keyboard?) {
        writeResponse(
            "send",
            "text" to text,
            "chatId" to chatId,
            "pixUrls" to pixUrls,
            "keyboard" to keyboard
        )
    }

    override fun sendPhotos(text: String, chatId: Long, attachments: List<String>) {
        writeResponse(
            "sendPhotos",
            "text" to text,
            "chatId" to chatId,
            "attachments" to attachments
        )
    }

    override fun getChatMembers(peer_id: Long, fields: List<String>): List<VkUser>? {
        writeResponse("getChatMembers", "peer_id" to peer_id, "fields" to fields)
        return readValueFromFile(pathToFile, "getChatMembers", listOf())
    }

    private inline fun <reified T> readValueFromFile(path: String, attributeName: String, defaultValue: T): T {
        val file = File("$path/$attributeName-in.txt")
        if (!file.exists()) return defaultValue

        val lines = file.readLines()
        if (lines.isEmpty()) return defaultValue

        val index = valuesReadCount.getOrPut(attributeName) { 0 } % lines.size
        valuesReadCount[attributeName] = index + 1
        val value = lines[index]
        return when(T::class) {
            Int::class -> value.toInt() as T
            Long::class -> value.toLong() as T
            String()::class -> value as T
            Boolean::class -> value.toBoolean() as T
            List::class -> {
                if (T::class.typeParameters == VkUser::class) {
                    Gson().fromJson(value, Array<VkUser>::class.java).toList() as T
                } else {
                    throw IllegalStateException("wrong type parameter")
                }
            }
            else -> throw IllegalStateException("wrong type")
        }
    }

    private fun writeResponse(methodName: String, vararg values: Pair<String, Any?>) {
        keeper.write(methodName, values.joinToString(separator = ",\n") { (v, k) -> "$v=$k" } )
    }
}