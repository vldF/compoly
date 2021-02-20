package chatbot.chatModules

import chatbot.ModuleObject
import chatbot.OnCommand
import chatbot.chatBotEvents.LongPollNewMessageEvent
import chatbot.chatModules.misc.Animal
import com.google.gson.JsonParser

@ModuleObject
object Dogs : Animal() {

    @OnCommand(["пёсик", "dog", "песик"], "ПЁСИКИ!", cost = 20)
    fun dog(event: LongPollNewMessageEvent) {
        animal(event)
    }

    override val animalApiLink: String
        get() = "https://dog.ceo/api/breeds/image/random"

    override fun parseAnimalResponse(response: String): String {
        return JsonParser().parse(response).asJsonObject["message"].asString
    }
}