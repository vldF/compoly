package modules.chatbot

@Target(AnnotationTarget.FUNCTION)
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
annotation class OnCommand(
        val commands: Array<String>,
        val description: String = "",
        val permissions: CommandPermission = CommandPermission.ALL,
        val cost: Int
)

@Target(AnnotationTarget.FUNCTION)
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
annotation class OnMessage