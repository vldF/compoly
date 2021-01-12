package chatbot.base

import api.VkPlatform
import api.keyboards.Keyboard
import api.objects.VkUser
import com.nhaarman.mockitokotlin2.*
import org.mockito.Answers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

interface VkApiMock {
    val meId: Long

    fun getUserIdByName(username: String): Long?

    fun getUserNameById(id: Long): String?

    fun kickUserFromChat(chatId: Long, userId: Long)

    fun isUserAdmin(chatId: Long, userId: Long): Boolean

    fun uploadPhotoByUrlAsAttachment(chatId: Long?, url: String): String?

    fun send(text: String, chatId: Long, pixUrls: List<String>, keyboard: Keyboard?)

    fun sendPhotos(text: String, chatId: Long, attachments: List<String>)

    fun getChatMembers(peer_id: Long, fields: List<String>): List<VkUser>?
}

fun getMock(api: VkApiMock): VkPlatform {
    val answer = Answer {
        api.executeMockMethod(it) ?: Answers.RETURNS_DEFAULTS.answer(it)
    }
    return mock(defaultAnswer = answer)
}

private fun VkApiMock.executeMockMethod(invocation: InvocationOnMock): Any? {
    val method = this.javaClass.methods.find { it.name == invocation.method.name } ?: invocation.method
    return method.invoke(this, *invocation.arguments)
}