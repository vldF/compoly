const val vkApiToken = "26d442de9c7849e58d21b346383fcc00d25660a91fa11addc8162c87a4661663532b1dccf0ca1447311ca"
const val key = "141ef67be66f26e2a199e2a98f0f34fd"
const val group_id = "188281612"
const val mainChatPeerId = 2000000002

const val testChatId = false
val chatIds = if (!testChatId) listOf(  // todo: remove 1?
    1,
    2,
    3
) else listOf(3)

const val testMode = false // no logging, no messages, println() instead
const val debugTime = false // used in timing.kt