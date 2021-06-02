package api

import api.keyboards.Keyboard
import api.objects.ChatMemberItemsInfo
import api.objects.VkUser
import configs.botId
import chatbot.chatModules.VirtualTargets
import chatbot.Attachment
import chatbot.GenerateMock
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import log
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.InputStreamBody
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.message.BasicNameValuePair
import configs.useTestMode
import configs.vkApiToken
import java.io.ByteArrayInputStream
import java.net.URL


object VkApi {
    private val client = HttpClientBuilder.create().build()
    private val gson = Gson()
    private val history = ApiHistory(4)
    // true if user is admin
    private val userAdminMap = mutableMapOf<Pair<Int, Int>, Boolean>()

    val meId: Int = botId // todo: get this value via API

    @GenerateMock(["username"], "1")
    fun getUserIdByName(username: String): Long? {
        val resp = post(
                "users.get", mutableMapOf(
                "user_ids" to username
            )
        )
        val json = resp?.asJsonObject
        return json?.get("response")?.asJsonArray?.get(0)?.asJsonObject?.get("id")?.asLong
    }

    @GenerateMock(["id"], "\"Test User\"")
    fun getUserNameById(id: Int): String? {
        val resp = post(
            "users.get", mutableMapOf(
                "user_ids" to id,
                "fields" to "screen_name"
            )
        )
        val json = resp?.asJsonObject
        return json
            ?.get("response")
            ?.asJsonArray
            ?.get(0)
            ?.asJsonObject
            ?.get("screen_name")
            ?.asString
            ?: VirtualTargets.getVirtualNameById(id)// hack for virtual mentions
    }

    @GenerateMock(["chatId", "userId"])
    fun kickUserFromChat(chatId: Int, userId: Int) {
        post("messages.removeChatUser", mutableMapOf(
                "chat_id" to chatId - 2000000000,
                "user_id" to userId
        ))
    }

    @GenerateMock(["chatId", "userId"], "false")
    fun isUserAdmin(chatId: Int, userId: Int): Boolean {
        val fromCache = userAdminMap[chatId to userId]
        if (fromCache != null) return fromCache

        val chatMembers = getChatMembersItems(chatId, listOf())
        val userInTheChat = chatMembers?.firstOrNull { it.member_id == userId }
        userAdminMap[chatId to userId] = userInTheChat?.is_admin == true

        return userInTheChat?.is_admin == true
    }

    @GenerateMock(["chatId", "url"], "\"photo_by_url_as_attachment\"")
    fun uploadPhotoByUrlAsAttachment(chatId: Int, url: String): String? {
        val imageConnection = URL(url).openConnection()
        val imageStream = imageConnection.getInputStream()
        return uploadPhoto(chatId, imageStream.readBytes())
    }

    private fun uploadDocByUrlAsAttachment(chatId: Int, url: String, fileName: String): String? {
        val docConnection = URL(url).openConnection()
        val docStream = docConnection.getInputStream()
        return uploadDoc(chatId, docStream.readBytes(), fileName)
    }

    @GenerateMock(["text", "chatId", "pixUrls", "keyboard", "removeDelay"], "null")
    fun send(
        text: String,
        chatId: Int,
        pixUrls: List<String> = listOf(),
        keyboard: Keyboard? = null,
        removeDelay: Long = -1
    ){
        val messageId = if (pixUrls.isEmpty()) {
            val params = mutableMapOf<String, Any>(
                    "message" to text,
                    "peer_ids" to chatId,
                    "random_id" to System.currentTimeMillis().toString()
            )
            if (keyboard != null) {
                params["keyboard"] = keyboard.getJson()
            }
            post("messages.send", params)
                ?.get("response")
                ?.asJsonArray?.get(0)
                ?.asJsonObject
                ?.get("conversation_message_id")
                ?.asInt

        } else {
            val attachments = mutableListOf<String>()
            for (url in pixUrls) {
                attachments.add(uploadPhotoByUrlAsAttachment(chatId, url) ?: "")
            }

            sendWithAttachments(text, chatId, attachments)
        }

        if (removeDelay != -1L && messageId != null) {
            GarbageMessagesCollector.deleteMessageWithDelay(
                messageId = messageId,
                chatId = chatId,
                delay = removeDelay
            )
        }
    }

    @GenerateMock(["text", "chatId", "attachments"], "null")
    fun sendWithAttachments(text: String, chatId: Int, attachments: List<String>): Int? {
        val res = post("messages.send", mutableMapOf(
            "message" to text,
            "peer_ids" to chatId,
            "random_id" to System.currentTimeMillis().toString(),
            "attachment" to attachments.joinToString(separator = ",")
        ))

        return res
            ?.get("response")
            ?.asJsonArray?.get(0)
            ?.asJsonObject
            ?.get("conversation_message_id")
            ?.asInt
    }

    fun getStringsOfAttachments(attachments: List<Attachment>, chatId: Int): List<String> {
        val strings = mutableListOf<String>()
        loop@ for (attachment in attachments) {
            log.info(attachment.toString())
            val stringOfAttachment = when (attachment.type) {
                "photo" -> {
                    val url = attachment.photo?.sizes?.last()?.url ?: continue@loop
                    uploadPhotoByUrlAsAttachment(chatId, url) ?: continue@loop
                }
                "audio" -> {
                    val audio = attachment.audio
                    if (audio?.access_key != null) {
                        "audio${audio.owner_id}_${audio.id}_${audio.access_key}"
                    } else {
                        "audio${audio?.owner_id}_${audio?.id}"
                    }
                }
                "video" -> {
                    val video = attachment.video
                    if (video?.access_key != null) {
                        "video${video.owner_id}_${video.id}_${video.access_key}"
                    } else {
                        "video${video?.owner_id}_${video?.id}"
                    }
                }
                "doc" -> {
                    val doc = attachment.doc
                    val url = doc?.url ?: continue@loop
                    uploadDocByUrlAsAttachment(chatId, url, "${doc.title}.${doc.ext}")
                }
                "poll" -> {
                    val poll = attachment.poll
                    if (poll?.access_key != null) {
                        "poll${poll.owner_id}_${poll.id}_${poll.access_key}"
                    } else {
                        "poll${poll?.owner_id}_${poll?.id}"
                    }
                }
                else -> ""
            }
            strings.add(stringOfAttachment.toString())
        }
        return strings
    }

    @GenerateMock(["peer_id", "fields"], "listOf()")
    fun getChatMembers(peer_id: Int, fields: List<String>): List<VkUser>? {
        val resp = post(
                "messages.getConversationMembers",
                mutableMapOf(
                        "peer_id" to peer_id,
                        "fields" to fields.joinToString(separator = ",")
                )
        ) ?: return null

        val vkResponse = resp["response"]?.asJsonObject ?: return null
        val profiles = vkResponse["profiles"].asJsonArray

        return profiles.map { gson.fromJson(it, VkUser::class.java) }
    }

    /* It isn't profiles!!! */
    private fun getChatMembersItems(peer_id: Int, fields: List<String>): List<ChatMemberItemsInfo>? {
        val resp = post(
                "messages.getConversationMembers",
                mutableMapOf(
                        "peer_id" to peer_id,
                        "fields" to fields.joinToString(separator = ",")
                )
        ) ?: return null

        val vkResponse = resp["response"]?.asJsonObject ?: return null
        val profiles = vkResponse["items"].asJsonArray

        return profiles.map { gson.fromJson(it, ChatMemberItemsInfo::class.java) }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun uploadPhoto(peer_id: Int?, data: ByteArray): String? {
        val serverData = post("photos.getMessagesUploadServer", mutableMapOf())

        val jsonServer = serverData?.asJsonObject ?: return null
        val vkResponse = jsonServer["response"]?.asJsonObject ?: throw IllegalStateException(serverData.asString)
        val uploadUrl = vkResponse["upload_url"]?.asString

        val inputStreamBody = InputStreamBody(ByteArrayInputStream(data), "iknt_top.jpg")

        val multipartData = MultipartEntityBuilder
            .create()
            .addPart("photo", inputStreamBody)
            .build()

        val requestUploadImage = HttpPost(uploadUrl)
        requestUploadImage.entity = multipartData

        val responseUpload = HttpClientBuilder
            .create()
            .build()
            .execute(requestUploadImage)
            .entity
            .content
            .readAllBytes()
            .decodeToString()

        val jsonUpload = JsonParser().parse(responseUpload).asJsonObject
        val server = jsonUpload["server"].asInt
        val photo = jsonUpload["photo"].asString
        val hash = jsonUpload["hash"].asString

        val saveData = post(
            "photos.saveMessagesPhoto",
            mutableMapOf(
                "server" to server,
                "photo" to photo,
                "hash" to hash
            )
        ) ?: return null

        val dataObject = saveData
            .asJsonObject["response"]
            .asJsonArray[0]
            .asJsonObject

        val ownerId = dataObject["owner_id"]
        val imageId = dataObject["id"]
        val accessKey = dataObject["access_key"].asString

        return "photo${ownerId}_${imageId}_$accessKey"
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun uploadDoc(peer_id: Int?, data: ByteArray, fileName: String): String? {
        val serverData =
            if (peer_id != null) post(
                "docs.getMessagesUploadServer",
                mutableMapOf("peer_id" to peer_id)
            ) else post("docs.getMessagesUploadServer", mutableMapOf())

        val jsonServer = serverData?.asJsonObject ?: return null
        val vkResponse = jsonServer["response"]?.asJsonObject ?: throw IllegalStateException(serverData.asString)
        val uploadUrl = vkResponse["upload_url"]?.asString
        log.info(uploadUrl)
        log.info(fileName)
        val inputStreamBody = InputStreamBody(ByteArrayInputStream(data), fileName)

        val multipartData = MultipartEntityBuilder
            .create()
            .addPart("file", inputStreamBody)
            .build()

        val requestUploadFile = HttpPost(uploadUrl)
        requestUploadFile.entity = multipartData

        val responseUpload = HttpClientBuilder
            .create()
            .build()
            .execute(requestUploadFile)
            .entity
            .content
            .readAllBytes()
            .decodeToString()

        val jsonUpload = JsonParser().parse(responseUpload).asJsonObject
        val file = jsonUpload["file"].asString
        val saveData = post(
            "docs.save",
            mutableMapOf(
                "file" to file
            )
        )
        if (saveData == null) {
            log.info("EMPTY SAVE DATA")
            return ""
        }

        val jsonAnswer = saveData
            .asJsonObject["response"]
            .asJsonObject["doc"]
            .asJsonObject
        val ownerId = jsonAnswer["owner_id"]
        val docId = jsonAnswer["id"]
        return "doc${ownerId}_${docId}"
    }

    fun deleteMessage(messageId: Int, chatId: Int) {
        val resp = post("messages.delete", mutableMapOf(
            "conversation_message_ids" to messageId,
            "delete_for_all" to 1,
            "peer_id" to chatId
        ))

        println(resp)
    }


    @Suppress("SameParameterValue")
    @OptIn(ExperimentalStdlibApi::class)
    fun post(methodName: String, params: MutableMap<String, Any>): JsonObject? {
        log.info("vk api query: methodName=$methodName, params=$params")
        history.use(methodName)
        if (useTestMode && methodName == "messages.send") return null

        val reqParams = mutableListOf<BasicNameValuePair>()
        reqParams.add(BasicNameValuePair("access_token", vkApiToken))
        reqParams.add(BasicNameValuePair("v", "5.155"))
        for ((p, v) in params) {
            reqParams.add(BasicNameValuePair(p, v.toString()))
        }

        val request = HttpPost("https://api.vk.com/method/$methodName")
        request.entity = UrlEncodedFormEntity(reqParams, charset("utf-8"))
        val response = client.execute(request).entity.content.readAllBytes()?.decodeToString()
        log.info("response: $response")
        request.releaseConnection()

        return JsonParser().parse(response).asJsonObject
    }
}
