package chatbot.chatModules

import chatbot.ModuleObject
import chatbot.OnCommand
import chatbot.UsageInfo
import chatbot.chatBotEvents.LongPollNewMessageEvent
import chatbot.chatModules.misc.Animal
import com.google.gson.JsonParser

@ModuleObject
object Dogs : Animal() {
    private const val notEnoughMessage = "Товарищ, ваши пёсики закончились. Обновление запаса пёсиков происходит раз в 12 часов"

    @UsageInfo(baseUsageAmount = 1, levelBonus = 1, notEnoughMessage)
    @OnCommand(["пёсик", "dog", "песик"], "ПЁСИКИ!")
    fun dog(event: LongPollNewMessageEvent) {
        animal(event)
    }

    override val animalApiLink: String
        get() = "https://dog.ceo/api/breeds/image/random"

    override fun parseAnimalResponse(response: String): String {
        return JsonParser().parse(response).asJsonObject["message"].asString
    }
}