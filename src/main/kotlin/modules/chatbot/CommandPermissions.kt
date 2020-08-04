package modules.chatbot


enum class CommandPermission(val helpHeaderString: String) {
    USER("Общедоступные"),
    ADMIN("Доступные только для администраторов")
}