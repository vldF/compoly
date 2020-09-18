import database.hikari
import chatbot.ChatBot
import modules.events.EventStream
import modules.loops.LoopStream
import org.jetbrains.exposed.sql.Database

fun main() {
    EventStream.run()
    LoopStream.run()

    //initializing db
    Database.connect(hikari())

    ChatBot.start()
}