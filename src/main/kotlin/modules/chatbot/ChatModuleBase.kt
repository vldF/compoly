package modules.chatbot

class ChatModuleBase {

}

enum class CommandPermission(val helpHeaderString: String) {
    ALL("Общедоступные"),
    ADMIN_ONLY("Доступные только для администраторов")
}