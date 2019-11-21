package modules.weather
import log
import java.net.HttpURLConnection
import java.net.URL

const val server = "http://api.openweathermap.org/data/2.5/weather?id=498817&APPID=141ef67be66f26e2a199e2a98f0f34fd"
fun weatherGet(address: String): String? {
    try {
        val url = URL(address)
        with(url.openConnection() as HttpURLConnection) {
            requestMethod = "GET"  // optional default is GET
            log.info("Sent 'GET' request to $url, [$responseCode]")
            return inputStream.bufferedReader().readText()
        }
    } catch (e: Exception) {
        log.warning("Unable to get $address")
        return null
    }
}

//fun main() {
//    println(weatherGet(server))
//}