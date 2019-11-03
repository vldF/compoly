package modules.pageChecking

import java.io.File
import java.io.FileNotFoundException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Paths

class PageChecker : modules.Module {
    override val callingType = 0
    override val millis = 7 * 60 * 60L
    override val name = "Проверка обновления страницы"
    override var lastCalling = System.currentTimeMillis() + 3 * 60 * 60 * 1000L
    override fun call() {

        fun sendGet(address: String): String? {
            try {
                val url = URL(address)
                with(url.openConnection() as HttpURLConnection) {
                    requestMethod = "GET"  // optional default is GET
                    println("\nSent 'GET' request to URL : $url; Response Code : $responseCode")
                    return inputStream.bufferedReader().readText()
                }
            } catch (e: Exception) {
                return null
            }
        }

        fun isUpdated(page: String): Boolean? {

            val filePath = "/data/savedPages/" + page.replace("/", "_") + ".txt"
            val path = Paths.get("").toAbsolutePath().toString() + filePath

            val newPage = sendGet(page)
            if (newPage == null) {
                println("Не удается получить доступ к странице $page")
                return null
            }

            return try {
                if (newPage != File(path).readText()) {
                    File(path).bufferedWriter().use { it.write(newPage) }
                    println("Файл $filePath обновлен")
                    true
                } else {
                    false
                }
            } catch (e: FileNotFoundException) {
                println("Файл $filePath не найден")
                null
            }

        }

        val pages = listOf("http://sergei-sabonis.ru/Student/20192020/dm2019.htm")
        for (page in pages) {
            if (isUpdated(page) == true) {
                println("Страница $page была обновлена") //вывод в ВК, можно добавить время и т.п.
            }
        }

    }
}