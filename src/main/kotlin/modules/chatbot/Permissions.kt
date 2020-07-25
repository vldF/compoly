package modules.chatbot

import api.VkPlatform
import modules.chatbot.chatBotEvents.LongPollNewMessageEvent
import modules.chatbot.chatBotEvents.Platform

object Permissions {
    var vk = VkPlatform()
    fun getUserPermissionsByNewMessageEvent(event: LongPollNewMessageEvent) = when(event.platform) {
        Platform.VK -> getPermissionsVk(event.userId, event.chatId)
        Platform.DISCORD -> getPermissionsDiscord()
        Platform.TELEGRAM -> getPermissionsTelegram()
    }

    private fun getPermissionsVk(userId: Long, chatId: Long): CommandPermission {
        val users = vk.getChatMembers(chatId, listOf()) ?: return CommandPermission.ALL
        //Find Admin
        for (item in users) {
            if (item.is_admin && item.member_id == userId) {
                return CommandPermission.ADMIN_ONLY
            }
        }
        //Если не высшие права(админ), то что-то из рейтинговой системы
        return CommandPermission.ALL
    }

    private fun getPermissionsTelegram(): CommandPermission {
        return CommandPermission.ALL
    }

    private fun getPermissionsDiscord(): CommandPermission {
        return CommandPermission.ALL
    }
}