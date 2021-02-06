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
    val meId: Int

    fun getUserIdByName(username: String): Long?

    fun getUserNameById(id: Int): String?

    fun kickUserFromChat(chatId: Int, userId: Int)

    fun isUserAdmin(chatId: Int, userId: Int): Boolean

    fun uploadPhotoByUrlAsAttachment(chatId: Int?, url: String): String?

    fun send(text: String, chatId: Int, pixUrls: List<String>, keyboard: Keyboard?)

    fun sendPhotos(text: String, chatId: Int, attachments: List<String>)

    fun getChatMembers(peer_id: Int, fields: List<String>): List<VkUser>?
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