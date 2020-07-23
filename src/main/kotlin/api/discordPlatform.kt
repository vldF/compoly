package api

import disApiToken
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.MessageBuilder
import java.io.File

class DiscordPlatform : PlatformApiInterface {
    private val client = JDABuilder.createDefault(disApiToken).build()

    override fun send(text: String, chatId: Long, attachments: List<String>) {
        client.getTextChannelById(chatId)?.sendMessage(MessageBuilder(text).build()).let {
            for (attachment in attachments) {
                it?.addFile(File(attachment))
            }
        }
    }
    override fun getUserIdByName(username: String): Long? = client.getUsersByName(username, false).first().id.toLong()

    override fun kickUserFromChat(chatId: Long, userId: Long) {
    }

    override fun getUserNameById(id: Long): String? = client.getUserById(id).toString()

    override fun uploadPhoto(chatId: Long, data: ByteArray): String? {
        client.getTextChannelById(chatId)?.sendFile(data, "")
        return ""
    }
}