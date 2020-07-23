package api.objects

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter


class DiscordListener : ListenerAdapter() {
    override fun onMessageReceived(event: MessageReceivedEvent) {
        val msg = event.message
        if (msg.contentRaw.startsWith("/")) {
            val channel = event.channel
        }
    }
}