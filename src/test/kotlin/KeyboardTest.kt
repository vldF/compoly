import api.keyboards.KeyboardBuilder
import api.keyboards.KeyboardButton
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test


class KeyboardTest {
    @Test
    fun vk() {
        val keyboard = KeyboardBuilder()
                .addButton(KeyboardButton("test1"))
                .addButton(KeyboardButton("test2"))
                .addButton(KeyboardButton("test3"))
                .build()

        val json = keyboard.getVkJson()

        val rightJson = "{\"inline\":true,\"buttons\":[[{\"color\":\"primary\",\"action\":{\"type\":\"text\"" +
                ",\"label\":\"test1\",\"payload\":{\"callback\":\"test1\"}}},{\"color\":\"primary\",\"action\"" +
                ":{\"type\":\"text\",\"label\":\"test2\",\"payload\":{\"callback\":\"test2\"}}},{\"color\":\"primary\"" +
                ",\"action\":{\"type\":\"text\",\"label\":\"test3\",\"payload\":{\"callback\":\"test3\"}}}]]}"
        Assertions.assertEquals(rightJson, json)
    }
}