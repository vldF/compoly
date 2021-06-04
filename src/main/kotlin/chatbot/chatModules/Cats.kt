package chatbot.chatModules

import chatbot.ModuleObject
import chatbot.OnCommand
import chatbot.UsageInfo
import chatbot.chatBotEvents.LongPollNewMessageEvent
import chatbot.chatModules.misc.Animal
import com.google.gson.JsonParser
import configs.theCatApiKey

@ModuleObject
object Cats : Animal() {
    private const val notEnoughMessage = "Товарищ, ваши котики закончились. Обновление запаса котов происходит раз в 4 часа"

    @UsageInfo(baseUsageAmount = 1, levelBonus = 1, notEnoughMessage)
    @OnCommand(["котик", "cat"], "КОТИКИ!")
    fun cat(event: LongPollNewMessageEvent) {
        animal(event)
    }

    override val animalApiLink: String
        get() = "https://api.thecatapi.com/v1/images/search?api_key=$theCatApiKey"

    override fun parseAnimalResponse(response: String): String {
        return JsonParser().parse(response).asJsonArray[0].asJsonObject["url"].asString
    }
}

