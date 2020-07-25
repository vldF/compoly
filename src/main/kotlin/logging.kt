import java.util.logging.FileHandler
import java.util.logging.Logger
import java.util.logging.SimpleFormatter

val log: Logger by lazy {
    val log = Logger.getLogger("main")
    val logFile = FileHandler("main.log")
    log.addHandler(logFile)
    val formatter = SimpleFormatter()
    logFile.formatter = formatter
    log
}