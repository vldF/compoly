package api.keyboards

import com.google.gson.JsonObject

data class KeyboardButton (
        val callbackMessage: String,
        val showingName: String = callbackMessage,
        val color: KeyboardColor = KeyboardColor.PRIMARY
) {
    fun getVkJson(): JsonObject {
        val jsonObject = JsonObject()
        jsonObject.addProperty("color", color.colorName)

        val actionObject = JsonObject()
        actionObject.addProperty("type", "callback")
        actionObject.addProperty("label", showingName)

        val payload = JsonObject()
        payload.addProperty("callback", callbackMessage)

        actionObject.add("payload", payload)

        jsonObject.add("action", actionObject)
        return jsonObject
    }

    fun getTgJson(): JsonObject {
        val jsonObject = JsonObject()
        jsonObject.addProperty("text", showingName)
        jsonObject.addProperty("callback_data", callbackMessage)

        return jsonObject
    }
}

enum class KeyboardColor(val colorName: String) {
    PRIMARY("primary"),
    SECONDARY("secondary"),
    POSITIVE("positive"),
    NEGATIVE("negative")
}