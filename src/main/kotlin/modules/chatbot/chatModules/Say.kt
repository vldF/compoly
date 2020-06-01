package modules.chatbot.chatModules

import api.Vk
import modules.Active
import modules.chatbot.CommandPermission
import modules.chatbot.MessageNewObj
import modules.chatbot.OnCommand

@Active
class Say {
    private val vk = Vk()
    private val regex = Regex("(/.+) (\\d+) ([\\w\\W]+)+")

    @OnCommand(["say"],
        "отправить сообщение в чат с ID: /say ID СООБЩЕНИЕ",
        CommandPermission.ADMIN_ONLY,
        cost=0
    )
    fun say(message: MessageNewObj) {
        val text = message.text
        val regexed = regex.find(text)
        val receiverChatId = regexed?.groupValues?.get(2)?.let { Integer.parseInt(it) }
        val messageText = regexed?.groupValues?.get(3)

        if (receiverChatId == null) {
            vk.send("Неверный параметр ID", message.peer_id)
            return
        }
        if (messageText == null) {
            vk.send("Неверный параметр СООБЩЕНИЕ", message.peer_id)
            return
        }

        vk.send(messageText, receiverChatId)
    }
}