package manualTests.schedule

import base.*
import chatbot.chatModules.Schedule
import database.ScheduleTable
import database.dbQuery
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.junit.jupiter.api.Test

class ScheduleTest {
    @Test
    fun simpleTest() {
        prepareDb {
            it[text] = "test\nmultiline\ntext"
            it[days] = 100
            it[chatId] = 1
        }
        val keeper = ApiResponseKeeper()
        val path = "src/test/kotlin/manualTests/schedule/"
        val api = FiledVkApiMock(path, keeper)
        Schedule.onDay(getMock(api), 100)
        checkResults(path, keeper)
        destroyDB()
    }

    private fun prepareDb(dbPreparer: ScheduleTable.(InsertStatement<Number>) -> Unit) {
        initInmemoryDB()
        dbQuery {
            ScheduleTable.insert(dbPreparer)
        }
    }
}