import java.text.SimpleDateFormat

const val millisecondInDay = 24 * 60 * 60 * 1000L
const val timeZone = 3 * 60 * 60 * 1000L // Moscow timezone

private val myFormat = SimpleDateFormat("dd.MM.yyyy hh:mm:ss")
private val debugStartTime = myFormat.parse("10.05.2020 07:59:59").time //see debugTime in configs
private val timeDifference = System.currentTimeMillis() - debugStartTime

/**
 * @return local time
 */
fun getTime(): Long {
    return if (debugTime) {
        System.currentTimeMillis() + timeZone - timeDifference
    } else {
        System.currentTimeMillis() + timeZone
    }
}

/**
 * @return time since day's start
 */
fun getDayTime() = getTime() % millisecondInDay