package chatbot.chatModules

import com.google.gson.JsonParser
import chatbot.ModuleObject
import chatbot.OnCommand
import chatbot.chatBotEvents.LongPollNewMessageEvent

@ModuleObject
object Cats : Animal() {
    private const val theCatApiKey = "dc64b39c-51b6-43aa-ba44-a231e8937d5b" // todo: move it to config?

    @OnCommand(["котик", "cat"], "КОТИКИ!", cost = 20)
    fun cat(event: LongPollNewMessageEvent) {
        animal(event)
    }

    override val animalApiLink: String
        get() = "https://api.thecatapi.com/v1/images/search?api_key=$theCatApiKey"

    override fun parseAnimalResponse(response: String): String {
        return JsonParser().parse(response).asJsonArray[0].asJsonObject["url"].asString
    }
}

