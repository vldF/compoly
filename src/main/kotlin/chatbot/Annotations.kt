package chatbot

@Target(AnnotationTarget.CLASS)
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
annotation class ModuleObject

@Target(AnnotationTarget.FUNCTION)
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
annotation class OnCommand(
    val commands: Array<String>,
    val description: String = "",
    val permissions: CommandPermission = CommandPermission.USER,
    val showInHelp: Boolean = true,
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class UsageInfo(
    val baseUsageAmount: Int,
    val levelBonus: Int,
    val notEnoughMessage: String
)

@Target(AnnotationTarget.FUNCTION)
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
annotation class OnMessage

@Target(AnnotationTarget.FUNCTION)
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
annotation class OnPoll

@Target(AnnotationTarget.FUNCTION)
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
annotation class OnPollAnswer

@Target(AnnotationTarget.FUNCTION)
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
annotation class GenerateMock(val args: Array<String>, val defaultValue: String = "")