package api

import java.io.File

object TelegramUsersDataBase {
    private val file = loadUserIdFile()
    private var data = loadUserIds().toMutableMap()

    fun getIdByNick(nick: String) = data[nick]

    fun addId(id: Long) {
        val name = TelegramPlatform.getUserNameById(id) ?: return
        if (data[name] == id) return
        file.appendText("${name}:${id}\n")
        data[name] = id
    }

    fun addId(id: Long, name: String) {
        if (data[name] == id) return
        file.appendText("${name}:${id}\n")
        data[name] = id
    }

    fun updateAllNicknames() {
        val newData = data.map { (TelegramPlatform.getUserNameById(it.value) ?: "") to it.value }.toMap().toMutableMap()
        data = newData

        val newContent = StringBuffer()
        for ((name, id) in newData) {
            newContent.append("${name}:${id}\n")
        }
        file.writeText(newContent.toString())
    }

    private fun loadUserIdFile(): File {
        val file = File("tg_user_file")
        if (!file.exists()) {
            file.createNewFile()
        }

        return file
    }

    private fun loadUserIds() = file.readLines().map {
        it.split(":").let { p ->
            p[0] to p[1].toLong()
        }
    }.toMap()
}