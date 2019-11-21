package modules.weather
import api.Vk
import chatIds
import com.google.gson.Gson
import log
import modules.Module
import sendGet

const val key = "141ef67be66f26e2a199e2a98f0f34fd"
const val address = "http://api.openweathermap.org/data/2.5/weather?id=498817&units=metric&lang=ru&APPID=$key"

class Weather : Module {

    override val callingType = 1
    override val millis = 7 * 60 * 60L
    override val name = "Погода сейчас"
    override var lastCalling = 0L

    private val key = "141ef67be66f26e2a199e2a98f0f34fd"
    private val address = "http://api.openweathermap.org/data/2.5/weather?id=498817&units=metric&lang=ru&APPID=$key"

    private class Coord(val lon: Double, val lat: Double)
    private class Weather(val id: Int, val main: String, val description: String, val icon: String)
    private class Main(val temp: Double, val pressure: Int, val humidity: Int, val temp_min: Double, val temp_max: Double, val sea_level: Double, val grnd_level: Double)
    private class Wind(val speed: Double, val deg: Int)
    private class Clouds(val all: Int)
    private class Rain(val oneH: Float, threeH: Float)
    private class Snow(val oneH: Float, threeH: Float)
    private class Sys(val type: Int, val id: Int, val message: Double, val country: String, val sunrise: Int, val sunset: Int)
    private class Info (
        val coord: Coord,
        val weather: List<Weather>,
        val base: String,
        val main: Main,
        val visibility: Int,
        val wind: Wind,
        val clouds: Clouds,
        val rain: Rain,
        val snow: Snow,
        val dt: Int,
        val sys: Sys,
        val tymezone: Int,
        val id: Int,
        val name: String,
        val cod: Int
    )

    override fun call() {
        val json = sendGet(address)
        if (json != null) {
            val info = Gson().fromJson<Info>(json, Info::class.java)
            try {
                log.info("Weather casting...")
                val text =
                    """
                        ☀Погода сейчас☀
                        Температура: ${info.main.temp} °C
                        Описание: ${info.weather.first().description}
                        Ветер: ${info.wind.speed} м/с
                    """.trimIndent()
                Vk().send(text, chatIds)
            } catch (e: Exception) {
                log.warning("Weather casting exception")
            }
        }
    }
}