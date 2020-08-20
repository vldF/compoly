import database.hikari
import chatbot.ChatBot
import org.jetbrains.exposed.sql.Database

fun main() {
    //EventStream.run()
    //LoopStream.run()

    //initializing db
    Database.connect(hikari())

    ChatBot.start()
}