package configs

val vkApiToken by StringConfig()
val theCatApiKey by StringConfig()
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

val dbUserName by StringConfig()
val dbPassword by StringConfig()
val dbTable by StringConfig()
val dbIP by StringConfig()
val dbPort by IntConfig()
val dbType by StringConfig()
