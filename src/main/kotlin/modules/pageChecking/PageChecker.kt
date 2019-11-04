package modules.pageChecking

import java.io.File
import java.io.FileNotFoundException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Paths
import modules.Module

class PageChecker : Module {
    override val callingType = 0
    override val millis = 7 * 60 * 60L
    override val name = "Проверка обновления страницы"
    override var lastCalling = System.currentTimeMillis() + 3 * 60 * 60 * 1000L
    private val pages = listOf("http://sergei-sabonis.ru/Student/20192020/dm2019.htm") //Можно добавить сюда другие сайты

    private fun getPath(page: String): String {
        val filePath = "/data/savedPages/" + page.replace(Regex("""[\\?|"/.:<>*]"""), "_") + ".txt"
        return Paths.get("").toAbsolutePath().toString() + filePath
    }

    fun init() {
        for (page in pages) {
            val path = getPath(page)
            val text = sendGet(page)
            if (text != null) {
                File(path).writeText(text)
                println("Файл $path был создан")
            }
        }
    }

    private fun sendGet(address: String): String? {
        try {
            val url = URL(address)
            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"  // optional default is GET
                println("\nSent 'GET' request to URL : $url; Response Code : $responseCode")
                return inputStream.bufferedReader().readText()
            }
        } catch (e: Exception) {
            println("Не удается получить доступ к странице $address")
            return null
        }
    }

    private fun isUpdated(page: String): Boolean? {
        val path = getPath(page)
        val newPage = sendGet(page) ?: return null
        return try {
            if (newPage != File(path).readText()) {
                File(path).bufferedWriter().use { it.write(newPage) }
                println("Файл $path обновлен")
                true
            } else {
                false
            }
        } catch (e: FileNotFoundException) {
            println("Файл $path не найден")
            null
        }
    }

    override fun call() {
        for (page in pages) {
            if (isUpdated(page) == true) {
                println("Страница $page была обновлена") //вывод в ВК, можно добавить время и т.п.
            }
        }
    }
}