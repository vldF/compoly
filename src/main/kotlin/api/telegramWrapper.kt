package api

import api.objects.BaseUser
//import com.github.kotlintelegrambot.bot
import com.elbekD.bot.Bot
import telApiToken
import telBotUsername
import java.util.*

class TelegramPlatform: PlatformApiInterface {
    private val bot = Bot.createPolling(telBotUsername, telApiToken)
    private val chatIds = mutableListOf<Int>()
    private var botIsWorking = false

    private fun startBot() {
        bot.start()
        botIsWorking = true
    }

    override fun send(text: String, chatId: Int, attachments: List<String>) {
        if (!botIsWorking) startBot()
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
        if (!botIsWorking) startBot()
        for (chatId in chatIds) {
            val member = bot.getChatMember(chatId = chatId, userId =  id.toLong())
            if (member != null) return member.get().user.username
        }
        return null
    }

    override fun kickUserFromChat(chatId: Int, userId: Int) {
        if (!botIsWorking) startBot()
        bot.kickChatMember(chatId.toLong(), userId.toLong(), 0)
    }


}