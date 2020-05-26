package database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import javax.sql.DataSource

const val userName = "compoly"
const val password = "c0mp0ly"

object UserScore : Table() {
    val userId = integer("user_id")
    val chatId = integer("chat_id")
    val score = integer("score")
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