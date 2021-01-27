package chatbot.base

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject

class ApiResponseKeeper {
    private val storage = mutableMapOf<String, JsonArray>()
    private val prettyPrint = GsonBuilder().setPrettyPrinting().create()

    val usedApis: Collection<String>
        get() = storage.keys

    fun write(apiMethod: String, value: JsonObject) {
        val fromStorage = storage.getOrPut(apiMethod) { JsonArray() }
        fromStorage.add(value)
    }

    fun read(apiMethod: String): String {
        return prettyPrint.toJson(storage[apiMethod])
    }
}