package base

import api.VkPlatform
import chatbot.EventProcessor
import chatbot.chatBotEvents.LongPollEventBase
import chatbot.chatBotEvents.LongPollNewMessageEvent
import com.google.gson.Gson
import org.junit.jupiter.api.Assertions
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.ConcurrentLinkedQueue


private val ignoringFiles = setOf(
    "messages.txt"
)

private const val ignoreLine = "//ignore"

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
    checkTables(path)
}

fun afterTest() {
    destroyDB()
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
        val fileData = file.readText()
        if (fileData.isIgnore) continue

        val storedData = keeper.read(fileApiName)
            ?: throw IllegalStateException("test data file ${file.name} exists, but it's API have not been used")
        assertTextEquals(fileData, storedData, "file: ${file.name}")
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

fun checkTables(path: String) {
    val excepted = File(path).listFiles()?.filter { it.name.endsWith(".sql.dump") } ?: return

    val actual = dumpDB()

    for (exceptedDumpFile in excepted) {
        val tableName = exceptedDumpFile.name.removeSuffix(".sql.dump")
        val content = exceptedDumpFile.readText()

        if (content.isIgnore) continue

        val actualContent = actual[tableName]
        if (actualContent == null) {
            System.err.println("Table $tableName not fount in database")
            return
        }

        assertTextEquals(
            content,
            actualContent,
            "table unequals $tableName"
        )
    }

    val actualTableNames = actual.map { it.key }.toSet()
    val exceptedTableNames = excepted.map { it.name.removeSuffix(".sql.dump") }
    val uncheckedTableNames = actualTableNames - exceptedTableNames

    for (uncheckedTableName in uncheckedTableNames) {
        val content = actual[uncheckedTableName]!!
        File("$path/${uncheckedTableName}.sql.dump").writeText(content)
    }

    if (uncheckedTableNames.isNotEmpty()) {
        throw FileNotFoundException("Next sql dumps were created: ${uncheckedTableNames.joinToString(", ")}")
    }
}

fun assertTextEquals(excepted: String, actual: String, errorMessage: String = "") {
    Assertions.assertEquals(
        excepted.formatted,
        actual.formatted,
        errorMessage
    )
}

private val String.formatted
    get() = replace("\n\r", "\n").replace("\\n", "\n").replace(" ", "")

private fun loadMessages(path: String, api: VkPlatform): List<LongPollNewMessageEvent> {
    val content = File("$path/messages.txt").readText()
     return Gson()
         .fromJson(content, Array<Message>::class.java).map {
             val date = it.date ?: System.currentTimeMillis()
             LongPollNewMessageEvent(api, it.chatId, it.text, it.userId, it.forwardMessageFromId, date)
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
    val chatId: Int,
    val text: String,
    val userId: Int,
    val forwardMessageFromId: Int?,
    val date: Long?
)

private val String.isIgnore
    get() = toLowerCase().replace(" ", "").startsWith(ignoreLine)