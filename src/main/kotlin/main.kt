import api.SendMessageThread
import database.hikari
import modules.chatbot.ChatBot
import org.jetbrains.exposed.sql.Database

fun main() {
    //EventStream.run()
    //LoopStream.run()
    SendMessageThread.start()

    //initializing db
    Database.connect(hikari())

    ChatBot.start()
}