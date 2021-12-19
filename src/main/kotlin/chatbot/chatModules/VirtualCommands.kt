package chatbot.chatModules

import api.Command
import api.TextMessageParser
import chatbot.*
import chatbot.chatBotEvents.LongPollNewMessageEvent
import chatbot.listeners.VirtualCommandBody
import database.VirtualCommands
import database.dbQuery
import log
import org.jetbrains.exposed.sql.*
import java.util.concurrent.ConcurrentLinkedQueue

@ModuleObject
object VirtualCommands {
    private lateinit var virtualCommands: ConcurrentLinkedQueue<VirtualCommandBody>
    private val parser = TextMessageParser()

    init {
        initVirtualCommandListeners()
    }

    @OnCommand(["виртуальнаякоманда", "виртуальная_команда", "createvirtual"], permissions = CommandPermission.ADMIN)
    fun createCommand(event: LongPollNewMessageEvent) {
        val api = event.api
        val chatId = event.chatId
        val parsed = TextMessageParser().parse(event.text)
        val attachments = api.getStringsOfAttachments(event.attachments ?: listOf(), event.chatId)
        val attachmentsString = attachments.joinToString(",")
        if (parsed.size < 2) {
            api.send("Неверные аргументы товарищ", event.chatId)
            return
        }
        val commandName = parsed.data[1].rawText.toLowerCase()

        if (isCommonCommandExists(commandName)) {
            api.send("Невозможно переопределить базовую команду", chatId)
            return
        }

        val lines = event.text.lines()
        val text = if (lines.size > 1) {
            lines.subList(1, lines.size).joinToString("\n")
        } else ""

        if (text.isEmpty() && attachments.isEmpty()) {
            api.send("Неверные аргументы. NB: Текст должен начинаться с новой строки", chatId)
            return
        }

        var isUpdate = false

        val commandId = if (isVirtualCommandExists(commandName, chatId)) {
            dbQuery {
                isUpdate = true
                VirtualCommands.update ({ (VirtualCommands.chatId eq chatId) and (VirtualCommands.commandName eq commandName) }) {
                    it[this.textCommand] = text
                    it[this.attachments] = attachmentsString
                }
                VirtualCommands.select {
                    (VirtualCommands.chatId eq chatId) and (VirtualCommands.commandName eq commandName)
                }.first()[VirtualCommands.commandId]
            }
        } else {
            dbQuery {
                VirtualCommands.insert {
                    it[this.chatId] = event.chatId
                    it[this.commandName] = commandName
                    it[this.attachments] = attachmentsString
                    it[textCommand] = text
                }.resultedValues?.get(0)?.get(VirtualCommands.chatId)
            }
        }

        if (commandId == null) {
            api.send("Ошибка при создании команды", chatId)
            return
        }
        if (!isUpdate) {
            virtualCommands.add(
                VirtualCommandBody(
                    commandId,
                    chatId,
                    commandName.split(','),
                    text,
                    attachmentsString
                )
            )
        } else {
            virtualCommands.find { it.commandId == commandId }?.let {
                it.text = text
                it.attachments = attachmentsString
            }
        }
        log.info("added to SQL")
        if (attachments.isEmpty()) {
            api.send("Добавлена команда: $commandName", chatId)
        } else {
            api.sendWithAttachments("Добавлена команда: $commandName", chatId, attachments)
        }
    }

    private fun isVirtualCommandExists(name: String, chatId: Int): Boolean {
        return dbQuery {
            VirtualCommands.select {
                (VirtualCommands.chatId eq chatId) and (VirtualCommands.commandName eq name)
            }.firstOrNull() != null
        }
    }

    private fun isCommonCommandExists(name: String): Boolean {
        return EventProcessor.commandListeners.any { it.commands.contains(name) }
    }

    @OnCommand(["списоквиртуальныхкоманд", "список", "virtuallist", "list"])
    fun virtualList(event: LongPollNewMessageEvent) {
        val commands = virtualCommands
            .filter { command -> command.chatId == event.chatId }
            .joinToString(separator = "\n") { it.triggers[0] }
        event.api.send("Виртуальные команды:\n$commands", event.chatId)
    }

    @OnCommand(["удалить", "удалитьвиртуальнуюкоманду", "delete", "remove"], permissions = CommandPermission.ADMIN)
    fun removeCommand(event: LongPollNewMessageEvent) {
        val api = event.api
        val parsed = TextMessageParser().parse(event.text)
        if (parsed.size != 2) {
            api.send("Неверные аргументы, товарищ", event.chatId)
            return
        }

        val commandText = parsed.data[1].rawText
        var commandId = -1
        for (command in virtualCommands) {
            if (command.triggers.contains(commandText) && command.chatId == event.chatId) {
                commandId = command.commandId
                virtualCommands.remove(command)
                break
            }
        }

        if (commandId == -1) {
            api.send("Такой виртуальной команды не существует, товарищ", event.chatId)
            return
        }

        dbQuery {
            log.info("trying to delete id: $commandId...")
            VirtualCommands.deleteWhere { VirtualCommands.commandId eq commandId }
        }

        api.send("Удалена виртуальная команда: $commandText", event.chatId)
    }

    @OnMessage
    fun executeVirtualCommandIfExists(event: LongPollNewMessageEvent) {
        val command = parser.parse(event.text).get<Command>(0)?.name?.toLowerCase() ?: return
        log.info("trying find virtual command...")
        for (virtualCommand in virtualCommands) {
            if (virtualCommand.triggers.contains(command)) {
                sendVirtualCommandAnswer(event, virtualCommand)
            }
        }
    }

    private fun initVirtualCommandListeners() {
        log.info("init virtual commands...")
        dbQuery {
            virtualCommands = ConcurrentLinkedQueue()
            for (command in VirtualCommands.selectAll()) {
                val body = VirtualCommandBody(
                    command[VirtualCommands.commandId],
                    command[VirtualCommands.chatId],
                    command[VirtualCommands.commandName].split(","),
                    command[VirtualCommands.textCommand],
                    command[VirtualCommands.attachments]
                )
                virtualCommands.add(body)
            }
        }
    }

    private fun sendVirtualCommandAnswer(event: LongPollNewMessageEvent, virtualCommandBody: VirtualCommandBody) {
        val chatId = virtualCommandBody.chatId
        val text = virtualCommandBody.text
        val attachments = listOf(virtualCommandBody.attachments)
        if (event.chatId == chatId) {
            event.api.sendWithAttachments(text, chatId, attachments)
        }
    }

}