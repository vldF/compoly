package api.keyboards

import com.google.gson.JsonArray
import com.google.gson.JsonObject

class Keyboard (private val buttons: Collection<KeyboardButton>) {
    fun getVkJson(): String {
        val jsonObject = JsonObject()
        jsonObject.addProperty("inline", true)

        val jsonListOfButtons = JsonArray(buttons.size)
        buttons.forEach{ jsonListOfButtons.add(it.getVkJson()) }

        val arrayOfArrayOfButtons = JsonArray()
        arrayOfArrayOfButtons.add(jsonListOfButtons)

        jsonObject.add("buttons", arrayOfArrayOfButtons)

        return jsonObject.toString()
    }
}