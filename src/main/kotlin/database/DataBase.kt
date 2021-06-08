package database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import configs.dbIP
import configs.dbPassword
import configs.dbPort
import configs.dbTable
import configs.dbType
import configs.dbUserName
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import javax.sql.DataSource

const val EMPTY_HISTORY_TEXT = ""

object UserScore : Table() {
    val userId = integer("user_id")
    val chatId = integer("chat_id")
    val reputation = integer("score")
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

object ScheduleTable: Table("schedule") {
    val id = integer("id").autoIncrement()
    val chatId = integer("chat_id")
    val days = integer("days")
    val text = text("schedule_text")

    override val primaryKey = PrimaryKey(id)
}

fun hikari(): DataSource {
    val conf = HikariConfig()
    conf.jdbcUrl = "jdbc:$dbType://$dbIP:$dbPort/$dbTable"
    conf.username = dbUserName
    conf.password = dbPassword
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