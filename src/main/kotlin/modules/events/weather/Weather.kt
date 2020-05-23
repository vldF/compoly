package modules.events.weather
import api.Vk
import chatIds
import com.google.gson.Gson
import key
import log
import modules.events.Event
import modules.events.Time
import modules.sendGet
import kotlin.math.exp

const val address = "http://api.openweathermap.org/data/2.5/weather?id=498817&units=metric&lang=ru&APPID=$key"

class Weather : Event {
    override val schedule = listOf<Time>()
    override val name = "Погода сейчас"

    private val vk = Vk()

    private fun apparentTemperature(temperature: Double, wind: Double, humidity: Double): String {
        val e = (humidity / 100) * 6.105 * exp((17.27 * temperature) / (237.7 + temperature))
        return String.format("%.1f", temperature + 0.348 * e - 0.7 * wind - 4.25)
    }

    override fun call() {
        val json = sendGet(address)
        if (json != null) {
            val info = Gson().fromJson<Info>(json, Info::class.java)
            try {
                log.info("Weather casting...")
                val text =
                    """
                        🌤Погода сейчас: ${info.weather.first().description}
                        🌬Ветер: ${info.wind.speed.toInt()} м/с
                        ☁Облачность: ${info.clouds.all} %
                        🌡Температура: ${info.main.temp} °C
                        🖐Ощущается как: ${apparentTemperature(info.main.temp, info.main.temp, info.main.humidity.toDouble())} °C
                    """.trimIndent()
                vk.send(text, chatIds)
            } catch (e: Exception) {
                log.warning("Weather casting exception")
            }
        }
    }

    private class Coord(val lon: Double, val lat: Double)
    private class Weather(val id: Int, val main: String, val description: String, val icon: String)
    private class Main(val temp: Double, val pressure: Int, val humidity: Int, val temp_min: Double, val temp_max: Double, val sea_level: Double, val grnd_level: Double)
    private class Wind(val speed: Double, val deg: Int)
    private class Clouds(val all: Int)
    private class Rain(val oneH: Float, val threeH: Float)
    private class Snow(val oneH: Float, val threeH: Float)
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
}