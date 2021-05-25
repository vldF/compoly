package configs

import java.io.File
import java.util.*
import kotlin.reflect.KProperty

abstract class Config <T> {
    private val configFilePath = "./config.properties"
    private val configProperties = Properties()

    init {
        val configFile = File(configFilePath)
        if (!configFile.exists()) {
            val defaultConfig = javaClass.getResource("/$configFilePath")!!.readText()
            if (!configFile.createNewFile()) {
                throw IllegalStateException("Can not create config.properties")
            }
            configFile.writeText(defaultConfig)
        }
        configProperties.load(configFile.inputStream())
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val name = property.name
        val value = configProperties[name] ?: throw IllegalArgumentException("Unknown key: $name")
        return parseValue(value.toString())
    }

    abstract fun parseValue(value: String): T
}

class LongConfig : Config<Long>() {
    override fun parseValue(value: String): Long {
        return value.toLong()
    }
}

class IntConfig : Config<Int>() {
    override fun parseValue(value: String): Int {
        return value.toInt()
    }
}

class StringConfig : Config<String>() {
    override fun parseValue(value: String): String {
        return value
    }
}

class BooleanConfig : Config<Boolean>() {
    override fun parseValue(value: String): Boolean {
        return value.toBoolean()
    }
}