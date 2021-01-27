package chatbot.base

import database.UserScore
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
            val dump = exec("select * from $name") {
                builder.appendln(it.toString())
            }

            result[name] = builder.toString()
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
