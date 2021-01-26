package chatbot.base

import api.VkPlatform
import chatbot.EventProcessor
import chatbot.chatBotEvents.LongPollEventBase
import chatbot.chatBotEvents.LongPollNewMessageEvent
import com.google.gson.Gson
import org.junit.jupiter.api.Assertions
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue


private val ignoringFiles = setOf(
    "messages.txt"
)

fun runTest(path: String) {
    initInmemoryDB()
    loadAllTables(path)

    val keeper = ApiResponseKeeper()
    val api = FiledVkApiMock(path, keeper)
    val mock = getMock(api)
    val messages = loadMessages(path, mock)
    val queue = ConcurrentLinkedQueue<LongPollEventBase>()
    val eventProcessor = EventProcessor(queue)

    eventProcessor.initModules()

    for (message in messages) {
        eventProcessor.process(message)
    }
    checkResults(path, keeper)
}

private fun checkResults(path: String, keeper: ApiResponseKeeper) {
    val files = File(path)
        .listFiles(File::isFile)
        ?.filter { it.name !in ignoringFiles }
        ?.filter { it.name.endsWith("-out.txt") }
        ?.sorted()
        ?: throw IllegalStateException("empty data dir for test $path")

    val usedApis = keeper.usedApis
    for (file in files) {
        val fileApiName = file.apiName
        val storedData = keeper.read(fileApiName)
            ?: throw IllegalStateException("test data file ${file.name} exists, but it's API have not been used")
        val fileData = file.readText()
        Assertions.assertEquals(fileData, storedData, "file: ${file.name}")
    }

    val unexistsDataFiles = usedApis - files.map { it.apiName }
    for (apiName in unexistsDataFiles) {
        val storedData = keeper.read(apiName)!!
        val file = File("$path/$apiName-out.txt")
        file.createNewFile()
        file.writeText(storedData)
    }

    if (unexistsDataFiles.isNotEmpty()) {
        throw IllegalStateException("New files were created: ${unexistsDataFiles.joinToString { "$it.txt" }}")
    }
}

private fun loadMessages(path: String, api: VkPlatform): List<LongPollNewMessageEvent> {
    val content = File("$path/messages.txt").readText()
     return Gson()
         .fromJson(content, Array<Message>::class.java).map {
             LongPollNewMessageEvent(api, it.chatId, it.text, it.userId, it.forwardMessageFromId, it.date)
         }
         .toList()
}

private fun loadAllTables(dirPath: String) {
    val sqls = File(dirPath).listFiles()!!.filter { it.name.endsWith("sql") }
    for (file in sqls) {
        loadDBTable(file)
    }
}

private val File.apiName
    get() = name.removeSuffix("-out.txt")

data class Message(
    val chatId: Long,
    val text: String,
    val userId: Long,
    val forwardMessageFromId: Long?,
    val date: Int
)