package modules.chatbot.chatModules

import modules.Active
import modules.chatbot.CommandPermission
import modules.chatbot.ModuleObject
import modules.chatbot.OnCommand
import modules.chatbot.chatBotEvents.LongPollNewMessageEvent

@ModuleObject
object Say {
    private val regex = Regex("(/say) (\\d+) ([\\w\\W\\d]*)")

    @OnCommand(["say"],
        "отправить сообщение в чат с ID: /say ID СООБЩЕНИЕ",
        CommandPermission.ADMIN_ONLY
    )
    fun say(event: LongPollNewMessageEvent) {
        val api = event.api
        val text = event.text
        val regexed = regex.find(text)
        val receiverChatId = regexed?.groupValues?.get(2)?.toLongOrNull()
        val messageText = regexed?.groupValues?.get(3)

        if (receiverChatId == null) {
            api.send("Неверный параметр ID", event.chatId)
            return
        }
        if (messageText == null) {
            api.send("Неверный параметр СООБЩЕНИЕ", event.chatId)
            return
        }

        api.send(messageText, receiverChatId)
    }
}