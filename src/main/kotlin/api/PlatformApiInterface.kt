package api

import api.objects.BaseUser

interface PlatformApiInterface {
    fun send(text: String, chatId: Int, attachments: List<String> = listOf())

    fun getUserNameById(id: Int): String?

    fun getUserIdByName(username: String): Int?

    fun kickUserFromChat(chatId: Int, userId: Int)

    fun uploadPhoto(chatId: Int, data: ByteArray): String?

}
