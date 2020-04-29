package modules.events.happyBirthday

import api.Vk
import chatIds
import com.google.gson.Gson
import modules.Active
import modules.events.Event
import modules.events.Time
import java.util.*

const val PEER_ID = "2000000002"

@Active
class HappyBirthday : Event {

    override val schedule = listOf(Time(9, 0))
    override val name = "Birthday today"

    private data class JsonVK(val response: Response)
    private data class Response(
        val items: List<Item>,
        val count: Int,
        val profiles: List<Profile>,
        val groups: List<Group>
    )

    private data class Item(
        val member_id: Int,
        val can_kick: Boolean,
        val invited_by: Int,
        val join_date: Int,
        val is_admin: Boolean,
        val is_owner: Boolean,
        val domain: String,
        val bdate: String?
    )

    private data class Profile(
        val id: Int,
        val first_name: String,
        val last_name: String,
        val is_closed: Boolean,
        val can_access_closed: Boolean,
        val domain: String,
        val bdate: String?
    )

    private data class Group(
        val id: Int,
        val name: String,
        val screen_name: String,
        val is_closed: Int,
        val type: String,
        val is_admin: Boolean,
        val is_member: Boolean,
        val is_advertiser: Boolean,
        val photo_50: String,
        val photo_100: String,
        val photo_200: String
    )

    private fun getProfiles(): List<Profile> {
        val json = Vk().getConversationMembersByPeerID(PEER_ID, listOf("bdate", "domain"))
        return Gson().fromJson(json, JsonVK::class.java).response.profiles
    }

    override fun call() {
        val cal = Calendar.getInstance()
        val profiles = getProfiles()
        val currentDate = "${cal.get(Calendar.DAY_OF_MONTH)}.${cal.get(Calendar.MONTH) + 1}"
        val needToCongratulate = mutableListOf<String>()
        for (profile in profiles) {
            var date: String = profile.bdate ?: continue
            date = date.split(".").subList(0, 2).joinToString(separator = ".")
            if (date == currentDate) {
                needToCongratulate.add("@${profile.domain}")
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
            Vk().send(message, chatIds)
        }
    }
}