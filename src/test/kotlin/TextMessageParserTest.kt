import api.*
import chatbot.chatBotEvents.Platform
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TextMessageParserTest {
    @Test
    fun simpleTest() {
        val vkParser = TextMessageParser(Platform.VK)
        val text = "/cat покажи самого крутого котика! Потрать мои 20 e-баллов !"
        val res = vkParser.parse(text)

        Assertions.assertEquals(10, res.size)
        Assertions.assertEquals(res.getTextSlice(0, 9), text)
        Assertions.assertTrue(res.isObjectOnIndexHasType(0, Command::class))
        Assertions.assertTrue(res.isObjectOnIndexHasType(7, IntegerNumber::class))
        Assertions.assertTrue(res.isObjectOnIndexHasType(1, Text::class))
    }

    @Test
    fun simpleVkMentionTest() {
        val vkParser = TextMessageParser(Platform.VK)
        val text = "/testCommand [id156594337|@mrvladf]"
        val res = vkParser.parse(text)

        Assertions.assertEquals(2, res.size)
        Assertions.assertTrue(res.isObjectOnIndexHasType(1, Mention::class))

        val mention = res.get<Mention>(1)
        val mentionId = mention?.targetId
        val mentionScreenName = mention?.targetScreenName

        Assertions.assertEquals(156594337L, mentionId)
        Assertions.assertEquals("@mrvladf", mentionScreenName)
    }
}