package chatbot.chatModules

import api.GarbageMessagesCollector
import com.google.gson.JsonParser
import chatbot.ModuleObject
import chatbot.OnCommand
import chatbot.chatBotEvents.LongPollNewMessageEvent
import chatbot.chatModules.misc.Animal
import configs.theCatApiKey

@ModuleObject
object Cats : Animal() {
    private const val BASIC_USE_AMOUNT = 1
    private const val AMOUNT_MULTIPLIER = 1

    @OnCommand(["котик", "cat"], "КОТИКИ!")
    fun cat(event: LongPollNewMessageEvent) {
        val curCommandName = object : Any() {}.javaClass.enclosingMethod.name
        val canBeUsed = RatingSystem.canUseCommand(
            chatId = event.chatId,
            userId = event.userId,
            basicUseAmount = BASIC_USE_AMOUNT,
            amountMult = AMOUNT_MULTIPLIER,
            commandName = curCommandName
        )
        if (canBeUsed) {
            animal(event)
        } else {
            event.api.send(
                "Товарищ, ваши котики закончились. Обновление запаса котов происходит раз в 4 часа",
                event.chatId,
                removeDelay = GarbageMessagesCollector.DEFAULT_DELAY
            )
        }
    }

    override val animalApiLink: String
        get() = "https://api.thecatapi.com/v1/images/search?api_key=$theCatApiKey"

    override fun parseAnimalResponse(response: String): String {
        return JsonParser().parse(response).asJsonArray[0].asJsonObject["url"].asString
    }
}

