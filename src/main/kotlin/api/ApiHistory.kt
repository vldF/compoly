package api

class ApiHistory(private val defaultLimitation: Int) {
    private val methodsUsageHistory = mutableMapOf<String, List<Long>>()

    fun use(methodName: String, limitation: Int = -1) {
        val history = methodsUsageHistory[methodName]
        val timeNow = System.currentTimeMillis()
        if (history == null) {
            val newHistory = listOf(timeNow)
            methodsUsageHistory[methodName] = newHistory
        } else {
            val limit = if (limitation != -1) limitation else defaultLimitation
            val lastHistory = history.filter { timeNow - it < 1000 }
            if (lastHistory.size >= limit || timeNow - lastHistory.last() < limit) {
                Thread.sleep(((1 / limit.toDouble()) * 1000).toLong())
            }
            methodsUsageHistory[methodName] = lastHistory + timeNow
        }
    }

}
