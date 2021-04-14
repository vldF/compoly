package api.keyboards

import com.google.gson.JsonObject

data class KeyboardButton (
        val callbackMessage: String,
        val showingName: String = callbackMessage,
        val color: KeyboardColor = KeyboardColor.PRIMARY
) {
    fun getJson(): JsonObject {
        val jsonObject = JsonObject()
        jsonObject.addProperty("color", color.colorName)

        val actionObject = JsonObject()
        actionObject.addProperty("type", "text")
        actionObject.addProperty("label", showingName)

        val payload = JsonObject()
        payload.addProperty("callback", callbackMessage)

        actionObject.add("payload", payload)

        jsonObject.add("action", actionObject)
        return jsonObject
    }
}

enum class KeyboardColor(val colorName: String) {
    PRIMARY("primary"),
    SECONDARY("secondary"),
    POSITIVE("positive"),
    NEGATIVE("negative")
}