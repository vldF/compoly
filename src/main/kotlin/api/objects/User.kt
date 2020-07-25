package api.objects

abstract class BaseUser {
        abstract val id: Long
        abstract val name: String
        abstract val nick: String
}

data class VkUser(
        override val id: Long,
        override val name: String,
        override val nick: String,  // aka `domain`
        val first_name: String,
        val last_name: String,
        val is_closed: Boolean,
        val can_access_closed: Boolean,
        val bdate: String?,
        val online: Int?,
        val is_admin: Boolean,
        val member_id: Long
        ) : BaseUser()