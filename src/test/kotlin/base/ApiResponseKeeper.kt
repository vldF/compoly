package base

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject

class ApiResponseKeeper {
    private val storage = mutableMapOf<String, JsonArray>()
    private val prettyPrint = GsonBuilder().setPrettyPrinting().create()

    val usedApis: Collection<String>
        get() = storage.keys

    fun write(apiMethod: String, value: JsonObject) {
        if (apiMethod == "send") {
            value.remove("dynamicRemoveTime")
        }
        val fromStorage = storage.getOrPut(apiMethod) { JsonArray() }
        fromStorage.add(value)
    }

    fun read(apiMethod: String): String? {
        return if (storage.containsKey(apiMethod))
            prettyPrint.toJson(storage[apiMethod])
        else
            null
    }
}