import api.ApiHistory
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ApiHistoryTest {
    @Test
    fun basic() {
        val apiHistory = ApiHistory(2)
        val time0 = System.currentTimeMillis()
        apiHistory.use("method") // no limitation
        val time1 = System.currentTimeMillis()
        apiHistory.use("method") // no limitation
        val time2 = System.currentTimeMillis()
        apiHistory.use("method") // limitation
        val time3 = System.currentTimeMillis()
        apiHistory.use("method") // limitation
        val time4 = System.currentTimeMillis()

        Thread.sleep(1000)

        val time5 = System.currentTimeMillis()
        apiHistory.use("method") // no limitation
        val time6 = System.currentTimeMillis()

        Assertions.assertTrue(time2 - time0 < 15)
        Assertions.assertTrue(time3 - time2 >= 500)
        Assertions.assertTrue(time4 - time3 >= 500)
        Assertions.assertTrue(time6 - time5 < 15)
    }
}