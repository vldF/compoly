package modules.chatbot

import api.SendMessageThread
import api.Vk
import com.google.gson.Gson
import com.google.gson.JsonArray
import group_id
import io.github.classgraph.ClassGraph
import log
import mainChatPeerId
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

data class JsonVK(val response: Response?, val error: Error?) {
    data class Response(
            val key: String,
            val server: String,
            val ts: String
    )

    data class Error(
            val error_code: Int,
            val error_msg: String,
            val request_params: JsonArray
    )
}

data class JsonAnswer(
        val ts: String,
        val updates: List<UpdatePart>
) {
    data class UpdatePart(
            val type: String,
            val `object`: MessageNewObj,
            val group_id: Int,
            val event_id: String
    )
}

data class MessageNewObj(
        val date: Int,
        val from_id: Int,
        val id: Int,
        val out: Int,
        val peer_id: Int,
        val text: String,
        val conversation_message_id: Int,
        val fwd_messages: List<Any>,
        val important: Boolean,
        val random_id: Int,
        val attachments: List<Any>,
        val is_hidden: Boolean
)

object ChatBot: Thread() {
    private lateinit var server: String
    private lateinit var key: String
    private lateinit var ts: String
    private lateinit var commands: List<Command>
    private var isInit = false


    private fun initLongPoll() {
        val response = Vk().post(
                "groups.getLongPollServer",
                mutableMapOf(
                        "group_id" to group_id
                )
        )

        val jsonVK = Gson().fromJson(response, JsonVK::class.java)

        if (jsonVK.error != null || jsonVK.response == null) {
            log.severe("vk long poll connection error. ${jsonVK.error}\n")
            return
        }

        val responseBody = jsonVK.response

        server = responseBody.server
        key = responseBody.key
        ts = responseBody.ts

        commands = ClassGraph().enableAllInfo().whitelistPackages("modules.chatbot.chatModules")
                .scan().use { scanResult ->
                    scanResult.allClasses
                            .flatMap {
                                it.methodAndConstructorInfo.filter { method ->
                                    method.hasAnnotation(OnCommand::class.java.name)
                                }.map { method ->
                                    val loadedMethod = method.loadClassAndGetMethod()
                                    val annotation = loadedMethod.getAnnotation(OnCommand::class.java)
                                    Command(
                                            annotation.commands,
                                            annotation.description,
                                            it.loadClass().getConstructor().newInstance(),
                                            loadedMethod,
                                            annotation.permissions
                                    )
                                }
                            }
                }

        isInit = true
    }

    private fun longPollRequest(): HttpResponse<String?> {
        val wait = 25
        val request = HttpRequest.newBuilder()
                .uri(URI.create(server))
                .POST(HttpRequest.BodyPublishers.ofString("act=a_check&key=$key&ts=${ts}&wait=$wait"))
                .build()
        val client = HttpClient.newHttpClient()
        val response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        )
        //log.info("response: ${response.body()}")
        return response
    }

    override fun run() {
        log.info("Initialising ChatBot...")
        initLongPoll()
        while (!isInit) {
            sleep(10 * 1000) // 10 seconds
            initLongPoll()
        }

        while (true) {
            val response = longPollRequest().body()
            val jsonAnswer = Gson().fromJson(response, JsonAnswer::class.java)
            ts = jsonAnswer.ts
            for (update in jsonAnswer.updates) {
                if (update.type == "message_new"  && update.`object`.peer_id != mainChatPeerId ) {
                    if (update.`object`.text == "1") {
                        log.info("I WANT TO DELETE")
                        Vk().post("messages.delete", mutableMapOf(
                                "message_ids" to update.`object`.conversation_message_id-6000,
                                "group_id" to group_id,
                                "delete_for_all" to 1
                        ))
                    }
                }
                if (update.type == "message_new" && update.`object`.text.startsWith("/")
                    && update.`object`.peer_id != mainChatPeerId) {
                    log.info("Message which starts with \"/\" found")
                    commandParser(update.`object`)
                }
            }
        }
    }

    private fun commandParser(message: MessageNewObj) {
        val commandName = message.text.split(" ")[0].removePrefix("/")
        for (command in commands) {
            if (command.commands.any{ it == commandName }) {
                var userCanUseCommand = true
                val userPermission = getPermisson(message)
                if (userPermission.ordinal < command.permission.ordinal)
                    userCanUseCommand = false

                if (userCanUseCommand) command.call.invoke(command.baseClass, message) else {
                    val domain = Vk().getUserDomain(message.from_id.toString())
                    Vk().send("""
                        @${domain}, у Вас недостаточно прав для использования команды /${commandName}
                    """.trimIndent(), message.peer_id
                    )
                }
            }
        }
    }

    fun getCommands() = commands

    private fun getPermisson(message: MessageNewObj): CommandPermission {
        val json = Vk().getConversationMembersByPeerID(message.peer_id, listOf())
        val items = Gson().fromJson(json, api.JsonVK::class.java).response.items
        //Find Admin
        for (item in items) {
            if (item.is_admin && item.member_id == message.from_id) {
                return CommandPermission.ADMIN_ONLY
            }
        }
        //Если не высшие права(админ), то что-то из рейтинговой системы
        return CommandPermission.ALL
    }
}

fun main() {
    SendMessageThread.start()
    ChatBot.start()
}