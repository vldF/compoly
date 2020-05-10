package modules.chatbot

import api.SendMessageThread
import api.Vk
import com.google.gson.Gson
import com.google.gson.JsonObject
import group_id
import io.github.classgraph.ClassGraph
import log
import mainChatPeerId
import modules.chatbot.commands.Command
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
            val request_params: JsonObject
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
        )?.body()

        val jsonVK = Gson().fromJson(response, JsonVK::class.java)

        if (jsonVK.error != null || jsonVK.response == null) {
            log.severe("vk long poll connection error. ${jsonVK.error}\n")
            return
        }

        val responseBody = jsonVK.response

        server = responseBody.server
        key = responseBody.key
        ts = responseBody.ts

        commands = emptyList()
        ClassGraph().enableAllInfo().whitelistPackages("modules.chatbot.commands")
                .scan().use { scanResult ->
                    val filtered = scanResult.getClassesImplementing("modules.chatbot.Command")
                            .filter { classInfo ->
                                classInfo.hasAnnotation("modules.Active")
                            }
                    commands = filtered
                            .map { it.loadClass() }
                            .map { it.getConstructor().newInstance() }
                            .filterIsInstance<Command>()
                }

        isInit = true
    }

    private fun longPolRequest(): HttpResponse<String?> {
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
        log.info("response: ${response.body()}")
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
            val response = longPolRequest().body()
            val jsonAnswer = Gson().fromJson(response, JsonAnswer::class.java)
            ts = jsonAnswer.ts
            for (update in jsonAnswer.updates) {
                if (update.type == "message_new" && update.`object`.text.startsWith("/")
                    && update.`object`.peer_id != mainChatPeerId) {
                    log.info("Message which starts with \"/\" found")
                    commandParser(update.`object`)
                }
            }
        }
    }

    private fun commandParser(message: MessageNewObj) {
        val beginOfCommand = message.text.split(" ").first()
        for (command in commands) {
            if (beginOfCommand.equals(command.keyWord, ignoreCase = true)) {
                log.info("Calling <${command.keyWord}>")
                command.call(message)
            }
        }
    }
}

fun main() {
    SendMessageThread.start()
    ChatBot.start()
}