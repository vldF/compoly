package api

import com.elbekD.bot.Bot
import telApiToken
import telBotUsername
import java.lang.IllegalArgumentException

class TelegramPlatform (private val bot: Bot): PlatformApiInterface  {
    init {
        bot.start()
    }
    private val chatIds = mutableListOf<Int>()

    override fun send(text: String, chatId: Int, attachments: List<String>) {
        bot.sendMessage(chatId.toLong(), text)
        chatIds.add(chatId)
    }

    /*override fun getUserIdByName(showingName: String): Int? {
        for (chatId in chatIds) {
            val member = bot.getChatMember()
            if (member != null) return member.get().user.username
        }
        return null
    }*/

    override fun getUserNameById(id: Int): String? {
        for (chatId in chatIds) {
            try {
                val member = bot.getChatMember(chatId = chatId, userId =  id.toLong())
                return member.get().user.username
            } catch (e: IllegalArgumentException) {
                continue
            }
        }
        return null
    }

    override fun kickUserFromChat(chatId: Int, userId: Int) {
        bot.kickChatMember(chatId.toLong(), userId.toLong(), 0)
    }

}