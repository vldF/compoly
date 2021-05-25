import api.GarbageMessagesCollector
import database.hikari
import chatbot.ChatBot
import modules.events.EventStream
import modules.loops.LoopStream
import org.jetbrains.exposed.sql.Database

fun main() {
    EventStream.start()
    LoopStream.start()
    Database.connect(hikari())
    GarbageMessagesCollector().start()

    ChatBot.start()
}