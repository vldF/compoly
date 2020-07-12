package api.objects

abstract class BaseUser(
        open val id: Int,
        open val name: String,
        open val nick: String
)

data class VkUser(
        private val _id: Int,
        private val _name: String,
        private val _nick: String,  // aka `domain`
        val first_name: String,
        val last_name: String,
        val is_closed: Boolean,
        val can_access_closed: Boolean,
        val bdate: String?,
        val online: Int?,
        val is_admin: Boolean,
        val member_id: Int
        ) : BaseUser(_id, _name, _nick)