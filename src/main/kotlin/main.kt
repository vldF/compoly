import api.SendMessageThread
import database.hikari
import modules.chatbot.ChatBot
import modules.events.EventStream
import modules.loops.LoopStream
import org.jetbrains.exposed.sql.Database

fun main() {
    val eventStream = EventStream()
    val loopStream = LoopStream()
    eventStream.run()
    loopStream.run()
    SendMessageThread.start()

    //initializing db
    Database.connect(hikari())

    ChatBot.start()
}