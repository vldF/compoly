const val vkApiToken = "26d442de9c7849e58d21b346383fcc00d25660a91fa11addc8162c87a4661663532b1dccf0ca1447311ca"
const val key = "141ef67be66f26e2a199e2a98f0f34fd"
const val group_id = "188281612"
const val mainChatPeerId = 2000000002L

const val telApiToken = "1294496718:AAH4zTnxURHRth60AtJVvwpzlkY7OroWue4"
const val telTestToken = "1377903420:AAEyegzv9nB70mfroXRXjCRbAh-aSUlZZYw"
const val telBotUsername = "compoly"

//const val disApiToken = "NzM1NzM2MTU4ODUxNjk0NjQz.XxkyUQ.rK-9BHwckLgRyzPxOeGN3JsBgvM"
const val disApiToken = "NzMyNDE3MzQ5OTgyMDkzMzgy.Xw0Szw.U-zkN0_8ywgnUZEeTYjzyzgNbak"

const val useTestChatId = true
val chatIds = if (!useTestChatId) listOf(  // todo: remove 1?
    1L,
    2L,
    3L
) else listOf(3L)

const val testMode = false // no logging, no messages, println() instead
const val debugTime = false // used in timing.kt