package chatbot.chatBotEvents

import api.PlatformApiInterface

class LongPollNewPollAnswerEvent (
        override val platform: Platform,
        override val api: PlatformApiInterface,
        val userId: Long,
        val pollId: String,
        val optionIds: Array<Int>
): LongPollEventBase()