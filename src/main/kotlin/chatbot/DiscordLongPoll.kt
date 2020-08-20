package chatbot

import api.DiscordPlatform
import chatbot.chatBotEvents.*
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.util.concurrent.ConcurrentLinkedQueue

class DiscordLongPoll(private val queue: ConcurrentLinkedQueue<LongPollEventBase>): Thread() {
    private val discordListener = DiscordListener(DiscordPlatform, this)

    private class DiscordListener(private val platform: DiscordPlatform, private val poll: DiscordLongPoll) : ListenerAdapter() {
        override fun onMessageReceived(event: MessageReceivedEvent) {
            val msg = event.message
            if (msg.author.idLong == platform.meId) return
            val channel = event.channel
            val serverId = event.guild.idLong
            poll.queue.add(
                    LongPollDSNewMessageEvent(
                        Platform.DISCORD,
                        platform,
                        channel.idLong,
                        msg.contentRaw,
                        msg.author.idLong,
                        serverId
                    )
            )

        }
    }

    override fun run() {
        DiscordPlatform.addListener(discordListener)
    }
}