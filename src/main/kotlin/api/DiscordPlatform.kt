package api

import api.keyboards.Keyboard
import disApiToken
import log
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.MemberCachePolicy

object DiscordPlatform : PlatformApiInterface {
    private val client = JDABuilder.createDefault(disApiToken)
            .enableIntents(GatewayIntent.GUILD_MEMBERS)
            .setMemberCachePolicy(MemberCachePolicy.ALL)
            .build()

    override val meId: Long = client.selfUser.idLong

    override fun send(text: String, chatId: Long, pixUrls: List<String>, keyboard: Keyboard?) {
        log.info("send discord: msg(channel: $chatId): $text")
        val channel = client.getTextChannelById(chatId)
        val msg = MessageBuilder(text)
        for (attachment in pixUrls)
            msg.append(attachment)
        channel?.sendMessage(msg.build())?.queue()
    }

    override fun getUserIdByName(username: String): Long? = client.getUsersByName(username, false).first().idLong

    override fun kickUserFromChat(chatId: Long, userId: Long) {
        client.guilds.firstOrNull()?.ban(userId.toString(), 1)?.queue()
    }

    override fun getUserNameById(id: Long): String? = client.getUserById(id)?.name ?: "ЗАСЕКРЕЧЕНО"

    override fun isUserAdmin(chatId: Long, userId: Long): Boolean =
        client.guilds.firstOrNull()?.getMemberById(userId)?.roles?.any { it.name == "Админ" } == true


    fun uploadPhoto(chatId: Long, data: ByteArray) {
        client.getTextChannelById(chatId)?.sendFile(data, "cat")
    }

    fun addListener(listenerAdapter: ListenerAdapter) {
        client.addEventListener(listenerAdapter)
    }
}
