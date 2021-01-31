package chatbot.base

import database.UserScore
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
    }
}

fun destroyDB() {
    transaction {
        SchemaUtils.drop(UserScore)
    }
}

fun dumpDB(): Map<String, String> {
    val result = mutableMapOf<String, String>()

    transaction {
        val allTableNames = TransactionManager.current().db.dialect.allTablesNames()
        val builder = StringBuilder()
        for (name in allTableNames) {
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
