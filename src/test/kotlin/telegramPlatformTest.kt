
import api.TelegramPlatform
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TelegramPlatformTest {
    private val telegram = TelegramPlatform(telApiToken)
    private var lastUpdateId = 0
    private val TEST_CHAT_ID: Long = -426117826
    private val picUrl = "https://www.interfax.ru/ftproot/textphotos/2019/05/17/700gc.jpg"
    @Test
    fun send() {
        telegram.send("test1", TEST_CHAT_ID)
        var updates = telegram.getUpdates(lastUpdateId + 1) ?: emptyArray()
        lastUpdateId = updates.last().update_id
        Assertions.assertEquals("test1", updates.last().message?.text)
        Assertions.assertEquals(TEST_CHAT_ID, updates.last().message?.chat?.id)

        telegram.send("test2", TEST_CHAT_ID, listOf(picUrl))
        updates = telegram.getUpdates(lastUpdateId + 1) ?: emptyArray()
        lastUpdateId = updates.last().update_id
        Assertions.assertEquals("test2", updates.last().message?.text)
        Assertions.assertEquals(TEST_CHAT_ID, updates.last().message?.chat?.id)
    }
}