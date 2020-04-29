package modules.loops

/**
 * Interface for loops
 */
interface Loop {
    val name: String // Will be used in logging
    val delay: Long // Delay between calls in milliseconds
    fun call()
}