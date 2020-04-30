const val vkApiToken = "d6bace0aa1a78e4e542359d0128251f41cc6d7c4c2aec5ba751b5ac68f87cd1b3b7cf1be88b46ed283bf7"
const val key = "141ef67be66f26e2a199e2a98f0f34fd"

const val testChatId = false
val chatIds = if (!testChatId) listOf(
    "1",
    "2",
    "3"
) else listOf("3")

const val testMode = false //no logging, no messages, println() instead