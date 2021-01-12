package chatbot.base

class ApiResponseKeeper {
    private val storage = mutableMapOf<String, StringBuffer>()
    val usedApis: Collection<String>
        get() = storage.keys

    fun write(apiMethod: String, value: String) {
        val fromStorage = storage.getOrPut(apiMethod) { StringBuffer() }
        fromStorage
            .appendln(value)
            .appendln("===[blocks separator]===")
    }

    fun read(apiMethod: String) = storage[apiMethod]?.toString()
}