package api

import org.jetbrains.exposed.sql.Table

const val userName = "compoly"
const val password = "c0mp0ly"

object UserScore : Table() {
    val userId = integer("user_id")
    val chatId = integer("chat_id")
    val score = integer("score")
}
