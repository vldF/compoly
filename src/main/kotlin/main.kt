import database.hikari
import modules.chatbot.ChatBot
import org.jetbrains.exposed.sql.Database

fun main() {
    //EventStream.run()
    //LoopStream.run()


    //initializing db
    Database.connect(hikari())

    ChatBot.start()
}