package api

import disApiToken
import log
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.hooks.ListenerAdapter

object DiscordPlatform : PlatformApiInterface {
    private val client = JDABuilder.createDefault(disApiToken).build()

    override fun send(text: String, chatId: Long, attachments: List<String>) {
        log.info("send discord: msg(channel: $chatId): $text")
        val channel = client.getTextChannelById(chatId)
        val msg = MessageBuilder(text)
        for (attachement in attachments)
            msg.append(attachement)
        channel?.sendMessage(msg.build())?.queue()
    }

    override fun getUserIdByName(username: String): Long? = client.getUsersByName(username, false).first().idLong

    override fun kickUserFromChat(chatId: Long, userId: Long) {
    }

    override fun getUserNameById(id: Long): String? = client.getUserById(id).toString()

    fun uploadPhoto(chatId: Long, data: ByteArray) {
        client.getTextChannelById(chatId)?.sendFile(data, "cat")
    }

    fun addListener(listenerAdapter: ListenerAdapter) {
        client.addEventListener(listenerAdapter)
    }
}
