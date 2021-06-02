package chatbot.chatModules

import api.GarbageMessagesCollector
import chatbot.ModuleObject
import chatbot.OnCommand
import chatbot.chatBotEvents.LongPollNewMessageEvent
import chatbot.chatModules.misc.Animal
import com.google.gson.JsonParser

@ModuleObject
object Dogs : Animal() {
    private const val BASIC_USE_AMOUNT = 1
    private const val AMOUNT_MULTIPLIER = 1

    @OnCommand(["пёсик", "dog", "песик"], "ПЁСИКИ!")
    fun dog(event: LongPollNewMessageEvent) {
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
                "Товарищ, ваши собачки закончились. Обновление запаса собак происходит раз в 4 часа",
                event.chatId,
                removeDelay = GarbageMessagesCollector.DEFAULT_DELAY
            )
        }
    }

    override val animalApiLink: String
        get() = "https://dog.ceo/api/breeds/image/random"

    override fun parseAnimalResponse(response: String): String {
        return JsonParser().parse(response).asJsonObject["message"].asString
    }
}