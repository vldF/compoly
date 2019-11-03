fun isUpdated(): Boolean? {
    val address = "http://sergei-sabonis.ru/Student/20192020/dm2019.htm"
    try {
        val newPage = File(address).readText()
        if (newPage != File("oldPage.txt")) {
            File("oldPage.txt").bufferWriter().use { it.write(newPage) }
            return true
        } else {
            return false
        }
    } catch (e: java.nio.file.InvalidPathException) {
        print("Неправильный адрес")
    } finally {
        return null
    }
}