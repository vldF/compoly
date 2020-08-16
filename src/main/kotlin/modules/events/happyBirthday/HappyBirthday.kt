package modules.events.happyBirthday


import api.VkPlatform
import chatIds
import mainChatPeerId
import modules.Active
import modules.events.Event
import modules.events.Time
import java.util.*

@Active
class HappyBirthday : Event {
    override val schedule = listOf(Time(9, 0))
    override val name = "Birthday today"

    override fun call() {
        val cal = Calendar.getInstance()
        val profiles = VkPlatform.getChatMembers(mainChatPeerId, listOf("bdata", "domain"))
                ?: throw IllegalStateException()
        val currentDate = "${cal.get(Calendar.DAY_OF_MONTH)}.${cal.get(Calendar.MONTH) + 1}"
        val needToCongratulate = mutableListOf<String>()
        for (profile in profiles) {
            var date: String = profile.bdate ?: continue
            date = date.split(".").subList(0, 2).joinToString(separator = ".")
            if (date == currentDate) {
                needToCongratulate.add("@${profile.nick}")
            }
        }
        if (needToCongratulate.size >= 1) {
            val message = when (needToCongratulate.size) {
                1 -> """

                    🎁🎁🎁Вся партия поздравляет нашего товарища ${needToCongratulate[0]} c Днём Рождения!!!
                    Желаем ему благополучия и такого же упорства в развитии нашей Великой Партии!🎉🎉🎉
                """.trimIndent()
                else -> """

                    🎁🎁🎁Вся партия поздравляет наших товарищей ${needToCongratulate.joinToString(separator = ", ")} c Днём Рождения!!!
                    Желаем им благополучия и такого же упорства в развитии нашей Великой Партии!🎉🎉🎉
                """.trimIndent()
            }
            chatIds.forEach { VkPlatform.send(message, it) }
        }
    }
}