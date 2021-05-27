package base
import api.VkApi
import api.keyboards.Keyboard
import api.objects.VkUser
import com.nhaarman.mockitokotlin2.*
import org.mockito.Answers
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

//DO NO MODIFY THIS CODE MANUALLY!!!

interface VkApiMock {
    fun getUserIdByName(username: String?): Long?
    
    fun getUserNameById(id: Int?): String?
    
    fun kickUserFromChat(chatId: Int?, userId: Int?): Unit
    
    fun isUserAdmin(chatId: Int?, userId: Int?): Boolean?
    
    fun uploadPhotoByUrlAsAttachment(chatId: Int?, url: String?): String?
    
    fun send(text: String?, chatId: Int?, pixUrls: List<String>?, keyboard: Keyboard?, removeDelay: Long?): Unit
    
    fun sendWithAttachments(text: String?, chatId: Int?, attachments: List<String>?): Integer?
    
    fun getChatMembers(peer_id: Int?, fields: List<String>?): List<VkUser>?
    
}

fun getMock(api: VkApiMock): VkApi {
    val answer = Answer {
        api.executeMockMethod(it) ?: Answers.RETURNS_DEFAULTS.answer(it)
    }
    return mock(defaultAnswer = answer)
}

private fun VkApiMock.executeMockMethod(invocation: InvocationOnMock): Any? {
    val method = this.javaClass.methods.find { it.name == invocation.method.name } 
    return if (method != null) {
        method.invoke(this, *invocation.arguments)
    } else {
        invocation.callRealMethod()
    }
}
