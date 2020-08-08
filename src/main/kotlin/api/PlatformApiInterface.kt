package api

import api.keyboards.Keyboard

interface PlatformApiInterface {
    fun send(text: String, chatId: Long, pixUrls: List<String> = listOf(), keyboard: Keyboard? = null)

    fun getUserNameById(id: Long): String?

    fun getUserIdByName(username: String): Long?

    fun kickUserFromChat(chatId: Long, userId: Long)

    fun isUserAdmin(chatId: Long, userId: Long): Boolean

    val meId: Long
}
