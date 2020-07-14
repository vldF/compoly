package api

import api.objects.BaseUser

interface PlatformApiInterface {
    fun send(text: String, chatId: Int, attachments: List<String> = listOf())

    //fun getChatMembers(peer_id: Int, fields: List<String>): List<BaseUser>?

    //fun getUserIdByName(showingName: String): Int?

    fun getUserNameById(id: Int): String?

    fun kickUserFromChat(chatId: Int, userId: Int)
}
