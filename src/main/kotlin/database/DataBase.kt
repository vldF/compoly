package database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import javax.sql.DataSource

const val userName = "compoly"
const val password = "c0mp0ly"
const val EMPTY_HISTORY_TEXT = ""

object UserScore : Table() {
    val userId = long("user_id")
    val chatId = long("chat_id")
    val reputation = integer("reputation")
    val history_respects = text("history_respects")
    val history_disrespects = text("history_disrespects")
}

fun hikari(): DataSource {
    val conf = HikariConfig()
    conf.jdbcUrl = "jdbc:postgresql://130.61.203.95:9997/compoly"
    conf.username = userName
    conf.password = password
    conf.maximumPoolSize = 5
    conf.isAutoCommit = true
    conf.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
    return HikariDataSource(conf)
}

fun <T> dbQuery(block: () -> T): T {
    return transaction {
        addLogger(StdOutSqlLogger)
        block()
    }
}