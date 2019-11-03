package modules.sabonis

import java.io.File
import java.io.FileNotFoundException
import java.net.HttpURLConnection
import java.net.URL

const val page = "http://sergei-sabonis.ru/Student/20192020/dm2019.htm"

fun sendGet(address: String): String {
    val url = URL(address)
    with(url.openConnection() as HttpURLConnection) {
        requestMethod = "GET"  // optional default is GET
        println("\nSent 'GET' request to URL : $url; Response Code : $responseCode")
        return inputStream.bufferedReader().readText()
    }
}

fun isUpdated(): Boolean? {
    val path = ""
    val newPage = sendGet(page)
    return try {
        if (newPage != File("oldPage.txt").readText()) {
            File("oldPage.txt").bufferedWriter().use { it.write(newPage) }
            true
        } else {
            false
        }
    } catch (e: FileNotFoundException) {
        print("Указанный файл не найден")
        null
    }
}

fun main() {
    isUpdated()
}