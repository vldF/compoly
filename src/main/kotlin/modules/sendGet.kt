package modules

import log
import java.net.HttpURLConnection
import java.net.URL

fun sendGet(address: String): String? {
    try {
        val url = URL(address)
        with(url.openConnection() as HttpURLConnection) {
            requestMethod = "GET"  // optional default is GET
            // log.info("Sent 'GET' request to $url, [$responseCode]")
            return inputStream.bufferedReader().readText()
        }
    } catch (e: Exception) {
        log.warning("Unable to get $address")
        return null
    }
}