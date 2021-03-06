package base

import database.*
import org.h2.jdbc.JdbcResultSet
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

fun initInmemoryDB() {
    Database.connect("jdbc:h2:mem:regular;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")
    transaction {
        SchemaUtils.create(UserScore)
        SchemaUtils.create(UserReward)
        SchemaUtils.create(VirtualMentions)
        SchemaUtils.create(VirtualCommands)
        SchemaUtils.create(ScheduleTable)
    }
}

fun destroyDB() {
    transaction {
        SchemaUtils.drop(UserScore)
        SchemaUtils.drop(UserReward)
        SchemaUtils.drop(VirtualMentions)
        SchemaUtils.drop(VirtualCommands)
        SchemaUtils.drop(ScheduleTable)
    }
}

fun dumpDB(): Map<String, String> {
    val result = mutableMapOf<String, String>()

    transaction {
        val allTableNames = TransactionManager.current().db.dialect.allTablesNames()
        for (name in allTableNames) {
            val builder = StringBuilder()
            exec("select * from $name") {
                while (it.next()) {
                    var colIndex = 1
                    while (true) {
                            try {
                                builder.append((it as JdbcResultSet).get(colIndex))
                                builder.append(", ")
                                colIndex++
                            } catch (E: Exception) {
                                break
                            }
                    }
                    builder.appendln()
                }
            }

            if (builder.isNotEmpty()) {
                result[name] = builder.toString()
            }
        }
    }

    return result
}

fun loadDBTable(file: File) {
    val content = file.readText()

    transaction {
        val conn = TransactionManager.current().connection
        conn.executeInBatch(content.lines())
    }
}
