import api.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TextMessageParserTest {
    @Test
    fun simpleTest() {
        val vkParser = TextMessageParser()
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
        val vkParser = TextMessageParser()
        val text = "/testCommand [id156594337|@mrvladf]"
        val res = vkParser.parse(text)

        Assertions.assertEquals(2, res.size)
        Assertions.assertTrue(res.isObjectOnIndexHasType(1, Mention::class))

        val mention = res.get<Mention>(1)
        val mentionId = mention?.targetId
        val mentionScreenName = mention?.targetScreenName

        Assertions.assertEquals(156594337, mentionId)
        Assertions.assertEquals("@mrvladf", mentionScreenName)
    }

    @Test
    fun mentionWithSpaces() {
        val parser = TextMessageParser()
        val text = "/command [id123123|super cool user]"
        val parsed = parser.parse(text)
        val mention = parsed.get<Mention>(1)

        Assertions.assertEquals("super cool user", mention?.targetScreenName)
    }

    @Test
    fun numericTest() {
        val parser = TextMessageParser()
        val text = "Test это число 10 , а это -5"
        val parsed = parser.parse(text)
        val number1 = parsed.get<IntegerNumber>(3)
        val number2 = parsed.get<IntegerNumber>(7)

        Assertions.assertEquals(10, number1?.number)
        Assertions.assertEquals(-5, number2?.number)
    }

    @Test
    fun numericTest2() {
        val parser = TextMessageParser()
        val text = "Test это число 10, а это-5"
        val parsed = parser.parse(text)

        Assertions.assertEquals(6, parsed.size)
    }

    @Test
    fun hardMentionTest() {
        val parser = TextMessageParser()
        val text = "Текст[id123|Вот так вот]"
        val parsed = parser.parse(text)
        val mention = parsed.get<Mention>(1)

        Assertions.assertEquals("Вот так вот", mention?.targetScreenName)
        Assertions.assertEquals(123, mention?.targetId)
    }
}