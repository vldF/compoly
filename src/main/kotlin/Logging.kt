import java.util.logging.FileHandler
import java.util.logging.Logger
import java.util.logging.SimpleFormatter

fun getLogger(name: String): Logger {
    val log = Logger.getLogger(name)
    val logFile = FileHandler("main.log")
    log.addHandler(logFile)
    val formatter = SimpleFormatter();
    logFile.formatter = formatter
    return log
}