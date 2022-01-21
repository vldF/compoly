import api.GarbageMessagesCollector
import chatbot.ChatBot
import database.DataBase
import modules.events.EventStream
import modules.loops.LoopStream

fun main() {
    DataBase.start()
    EventStream.start()
    LoopStream.start()
    GarbageMessagesCollector().start()

    ChatBot.start()
}