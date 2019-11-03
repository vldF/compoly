package modules

interface Module {
    val name: String // Будет использоваться при логировании, если оно будет
    val callingType: Int // 0 - вызывается в нужное время (см. значение ниже),
                         // 1 - вызывается раз в некоторое количество миллисекунд (см. ниже)
    val millis: Long // Если callingType = 0, то это количество миллисекунд с начала дня
                     // Если callingType = 1, то это период вызова функции
    var lastCalling: Long // системное значение, присвоить значение текущего времени
                          // (System.currentTimeMillis() + 3 * 60 * 60 * 1000L)
    fun call()
}