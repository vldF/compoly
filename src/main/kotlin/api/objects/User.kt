package api.objects

abstract class BaseUser {
        abstract val id: Int
        abstract val name: String
        abstract val domain: String
}

data class VkUser(
        override val name: String,
        override val domain: String,  // при парсинге значение nick у всех null, поэтому следует использовать domain
        val first_name: String,
        val last_name: String,
        val is_closed: Boolean,
        val can_access_closed: Boolean,
        val bdate: String?,
        val online: Int?,
        val is_admin: Boolean,
        override val id: Int
        ) : BaseUser()