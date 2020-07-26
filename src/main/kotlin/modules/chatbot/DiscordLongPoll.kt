package modules.chatbot

import api.DiscordPlatform
import modules.chatbot.chatBotEvents.LongPollEventBase
import modules.chatbot.chatBotEvents.LongPollNewMessageEvent
import modules.chatbot.chatBotEvents.Platform
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.util.concurrent.ConcurrentLinkedQueue

class DiscordLongPoll(private val queue: ConcurrentLinkedQueue<LongPollEventBase>): Thread() {
    private val discordListener = DiscordListener(DiscordPlatform, this)

    private class DiscordListener(private val platform: DiscordPlatform, private val poll: DiscordLongPoll) : ListenerAdapter() {
        override fun onMessageReceived(event: MessageReceivedEvent) {
            val msg = event.message
            if (msg.contentRaw.startsWith("/")) {
                val channel = event.channel
                poll.queue.add(LongPollNewMessageEvent(Platform.DISCORD, platform, channel.idLong, msg.contentRaw, msg.author.idLong))
            }
        }
    }
}