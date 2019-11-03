package modules

interface Module {
    val name: String // Будет использоваться при логировании, если оно будет
    fun call()
}