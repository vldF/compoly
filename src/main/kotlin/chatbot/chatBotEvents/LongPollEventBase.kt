package chatbot.chatBotEvents

import api.VkApi

abstract class LongPollEventBase {
    abstract val api: VkApi
}

