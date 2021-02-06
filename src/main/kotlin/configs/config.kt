import configs.BooleanConfig
import configs.IntConfig
import configs.LongConfig
import configs.StringConfig

val vkApiToken by StringConfig()
val weatherKey by StringConfig()
val botId by IntConfig()
val mainChatPeerId by IntConfig()

val useTestChatId by BooleanConfig()
val chatIds = if (!useTestChatId) listOf(  // todo: remove 1?
    1,
    2,
    3
) else listOf(3)

val useTestMode by BooleanConfig() // no logging, no messages, println() instead
val useDebugTime by BooleanConfig() // used in timing.kt