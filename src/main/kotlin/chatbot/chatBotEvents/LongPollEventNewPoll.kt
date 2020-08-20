package chatbot.chatBotEvents

import api.PlatformApiInterface

class LongPollEventNewPoll (
        override val platform: Platform,
        override val api: PlatformApiInterface,
        val pollId: String
): LongPollEventBase()