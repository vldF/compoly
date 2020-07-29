
import api.TelegramPlatform
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TelegramPlatformTest {
    private val telegram = TelegramPlatform()
    private var lastUpdateId = 0
    private val TEST_CHAT_ID: Long = -426117826
    private val picUrl = "https://www.interfax.ru/ftproot/textphotos/2019/05/17/700gc.jpg"
    @Test
    fun send() {
        telegram.send("test1", TEST_CHAT_ID)
        var updates = telegram.getUpdates(lastUpdateId) ?: emptyArray()
        lastUpdateId = updates[0].update_id
        Assertions.assertEquals(updates[0].message?.text, "test1")
        Assertions.assertEquals(updates[0].message?.chat?.id, TEST_CHAT_ID)

        telegram.send("test2", TEST_CHAT_ID, listOf(picUrl))
        updates = telegram.getUpdates(lastUpdateId) ?: emptyArray()
        lastUpdateId = updates[0].update_id
        Assertions.assertEquals(updates[0].message?.text, "test2")
        Assertions.assertEquals(updates[0].message?.chat?.id, TEST_CHAT_ID)
    }
}