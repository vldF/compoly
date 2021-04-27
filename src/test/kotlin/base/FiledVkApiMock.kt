package base
import api.keyboards.Keyboard
import api.objects.VkUser
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File

//DO NO MODIFY THIS CODE MANUALLY!!!

class FiledVkApiMock(private val pathToFile: String,
private val keeper: ApiResponseKeeper
) : VkApiMock {
    private val gson = Gson()
    private val valuesReadCount = mutableMapOf<String, Int>()
    override fun getUserIdByName(username: String?): Long? {
        writeResponse("getUserIdByName", "username" to username)
        return readValueFromFile(pathToFile, "getUserIdByName", 1)
    }
    
    override fun getUserNameById(id: Int?): String? {
        writeResponse("getUserNameById", "id" to id)
        return readValueFromFile(pathToFile, "getUserNameById", "Test User")
    }
    
    override fun kickUserFromChat(chatId: Int?, userId: Int?): Unit {
        writeResponse("kickUserFromChat", "chatId" to chatId, "userId" to userId)
    }
    
    override fun isUserAdmin(chatId: Int?, userId: Int?): Boolean? {
        writeResponse("isUserAdmin", "chatId" to chatId, "userId" to userId)
        return readValueFromFile(pathToFile, "isUserAdmin", false)
    }
    
    override fun uploadPhotoByUrlAsAttachment(chatId: Integer?, url: String?): String? {
        writeResponse("uploadPhotoByUrlAsAttachment", "chatId" to chatId, "url" to url)
        return readValueFromFile(pathToFile, "uploadPhotoByUrlAsAttachment", "photo_by_url_as_attachment")
    }
    
    override fun send(text: String?, chatId: Int?, pixUrls: List<String>?, keyboard: Keyboard?): Unit {
        writeResponse("send", "text" to text, "chatId" to chatId, "pixUrls" to pixUrls, "keyboard" to keyboard)
    }
    
    override fun sendWithAttachments(text: String?, chatId: Int?, attachments: List<String>?): Unit {
        writeResponse("sendWithAttachments", "text" to text, "chatId" to chatId, "attachments" to attachments)
    }
    
    override fun getChatMembers(peer_id: Int?, fields: List<String>?): List<VkUser>? {
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
                try {
                    Gson().fromJson(value, Array<VkUser>::class.java).toList() as T
                } catch (e: Exception) {
                    throw IllegalStateException("wrong type parameter")
                }
            }
            else -> throw IllegalStateException("wrong type")
        }
    }

    private fun writeResponse(methodName: String, vararg values: Pair<String, Any?>) {
        val json = JsonObject()
        for ((key, value) in values) {
            json.add(key, gson.toJsonTree(value))
        }

        keeper.write(methodName, json )
    }
}
