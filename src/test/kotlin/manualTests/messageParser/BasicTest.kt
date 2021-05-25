package manualTests.messageParser

import api.TextMessageParser
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class BasicTest {
    @Test
    fun withWrongBrackets() {
        val message = "lorem ) ipsum ("
        val parser = TextMessageParser()
        val parsed = parser.parse(message)
        Assertions.assertEquals(4, parsed.size)
    }
}