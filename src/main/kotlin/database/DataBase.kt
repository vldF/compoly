package database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import javax.sql.DataSource

const val userName = "compoly"
const val password = "c0mp0ly"
const val EMPTY_HISTORY_TEXT = ""

object UserScore : Table() {
    val userId = integer("user_id")
    val chatId = integer("chat_id")
    val reputation = integer("reputation")
    val history_respects = text("history_respects")
    val history_disrespects = text("history_disrespects")
}

object UserReward : Table() {
    val userId = integer("user_id")
    val chatId = integer("chat_id")
    val rewardName = text("reward_name")
}

object VirtualMentions : Table() {
    val chatId = integer("chat_id")
    val name = text("name")
    val id = integer("id").autoIncrement()
}

object VirtualCommands: Table() {
    val commandId = integer("id").autoIncrement()
    val chatId = integer("chat_id")
    val commandName = text("trigger")
    val attachments = text("attachments")
    val textCommand = text("text")

    override val primaryKey = PrimaryKey(commandId)
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