package modules.chatbot


enum class CommandPermission(val helpHeaderString: String) {
    ALL("Общедоступные"),
    ADMIN_ONLY("Доступные только для администраторов")
}