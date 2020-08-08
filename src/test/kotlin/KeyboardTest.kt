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

        val rightJson = "{\"inline\":true,\"buttons\":[[{\"color\":\"primary\",\"action\":{\"type\":\"callback\"," +
                "\"label\":\"test1\",\"payload\":{\"callback\":\"test1\"}}},{\"color\":\"primary\"," +
                "\"action\":{\"type\":\"callback\",\"label\":\"test2\",\"payload\":{\"callback\":\"test2\"}}}," +
                "{\"color\":\"primary\",\"action\":{\"type\":\"callback\",\"label\":\"test3\",\"payload\":" +
                "{\"callback\":\"test3\"}}}]]}"
        Assertions.assertEquals(rightJson, json)
    }

    @Test
    fun tg() {
        val keyboard = KeyboardBuilder()
                .addButton(KeyboardButton("test1", "test button"))
                .addButton(KeyboardButton("test2", "test button"))
                .addButton(KeyboardButton("test3", "test button"))
                .build()

        val json = keyboard.getTgJson()

        val rightJson = "{\"inline_keyboard\":[[{\"text\":\"test button\",\"callback_data\":\"test1\"}," +
                "{\"text\":\"test button\",\"callback_data\":\"test2\"}," +
                "{\"text\":\"test button\",\"callback_data\":\"test3\"}]]}"
        Assertions.assertEquals(rightJson, json)
    }
}