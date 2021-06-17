package chatbot

import api.GarbageMessagesCollector.Companion.DEFAULT_DELAY
import chatbot.chatBotEvents.LongPollEventBase
import chatbot.chatBotEvents.LongPollNewMessageEvent
import chatbot.chatModules.Cats
import chatbot.chatModules.RatingSystem
import chatbot.listeners.CommandListener
import chatbot.listeners.MessageListener
import chatbot.listeners.PollAnswerListener
import chatbot.listeners.PollListener
import io.github.classgraph.ClassGraph
import krobot.api.invoke
import log
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors


class EventProcessor(private val queue: ConcurrentLinkedQueue<LongPollEventBase>) : Thread() {
    private val pollSize = 4
    private val poll = Executors.newFixedThreadPool(pollSize)
    private val commandRegex = Regex("^/([a-zA-Zа-яА-ЯёЁ_]+)")

    companion object {
        lateinit var commandListeners: List<CommandListener>
        lateinit var messageListeners: List<MessageListener>
        lateinit var pollAnswerListeners: List<PollAnswerListener>
        lateinit var pollListeners: List<PollListener>
    }

    private fun mainLoop() {
        while (true) {
            val event = queue.poll()

            if (event == null) {
                sleep(100)
                continue
            }
            poll.submit {
                process(event)
            }
        }
    }

    fun process(event: LongPollEventBase) {
        when (event) {
            is LongPollNewMessageEvent -> {
                try {
                    messageListeners.forEach { it.call.invoke(it.baseClass, event) }
                } catch (e: Exception) {
                    System.err.println("Error on processing messageListeners:")
                    e.printStackTrace()
                }
                val text = event.text
                if (text.isEmpty()) return
                if (!text.startsWith("/")) return
                log.info("new message: $text")

                val command = commandRegex.find(event.text)?.groupValues?.get(1)?.toLowerCase() ?: return

                for (module in commandListeners) {
                    if (module.commands.contains(command)) {
                        val canBeUsed = canCommandBeUsedNow(module, event)
                        if (
                           isPermissionEnough(module, event) && canBeUsed
                        ) {
                            try {
                                module.method.invoke(module.classInstance, event)
                                return
                            } catch (e: Exception) {
                                log.severe("Error on processing commandListeners:")
                                log.info("command: $text")
                                log.info(e.stackTraceToString())
                            }
                        }

                        if (!canBeUsed) {
                            event.api.send(module.notEnoughMessage, event.chatId, removeDelay = DEFAULT_DELAY)
                        }
                    }
                }
            }
        }
    }

    private fun isPermissionEnough(module: CommandListener, event: LongPollNewMessageEvent) =
        module.permission == CommandPermission.USER || module.permission <= Permissions.getUserPermissionsByNewMessageEvent(event)

    private fun canCommandBeUsedNow(module: CommandListener, event: LongPollNewMessageEvent): Boolean {
        if (!module.controlUsage) return true
        val levelBonus = module.levelBonus
        val basicUseAmount = module.baseUsageAmount

        return RatingSystem.canUseCommand(
            chatId = event.chatId,
            userId = event.userId,
            basicUseAmount = basicUseAmount,
            levelBonus = levelBonus,
            commandName = module.classInstance::class.qualifiedName.toString() + "." + (module.method::getName)()
        )
    }

    override fun run() {
        log.info("initializing modules")
        initModules()
        log.info("Modules: "+ commandListeners.joinToString(", ") { it.commands[0] })
        log.info("Starting event processor")

        mainLoop()
    }

    fun initModules() {
        ClassGraph().enableAllInfo().whitelistPackages("chatbot.chatModules")
            .scan().use { scanResult ->
                val classes = scanResult.allClasses.filter { it.hasAnnotation(ModuleObject::class.java.name) }

                commandListeners = classes
                    .filter { it.isPublic }
                    .flatMap { clazz ->
                        val constructor = clazz.loadClass().getDeclaredConstructor()

                        constructor.trySetAccessible()
                        val clazzInstance = constructor.newInstance()

                        clazz.methodAndConstructorInfo.filter { method ->
                            method.hasAnnotation(OnCommand::class.java.name)
                        }.map { method ->
                            val loadedMethod = method.loadClassAndGetMethod()
                            val mainAnnotation = loadedMethod.getAnnotation(OnCommand::class.java)
                            val usageInfoAnnotation = loadedMethod.getAnnotation(UsageInfo::class.java)
                            CommandListener(
                                commands = mainAnnotation.commands,
                                description = mainAnnotation.description,
                                classInstance = clazzInstance,
                                method = loadedMethod,
                                permission = mainAnnotation.permissions,
                                showInHelp = mainAnnotation.showInHelp,
                                controlUsage = usageInfoAnnotation != null,
                                baseUsageAmount = usageInfoAnnotation?.baseUsageAmount ?: 0,
                                levelBonus = usageInfoAnnotation?.levelBonus ?: 0,
                                notEnoughMessage = usageInfoAnnotation?.notEnoughMessage ?: ""
                            )
                        }
                    }

                messageListeners = classes.flatMap {
                    val clazz = it.loadClass().getDeclaredConstructor()
                    clazz.trySetAccessible()
                    val clazzInstance = clazz.newInstance()
                    it.methodAndConstructorInfo.filter { method ->
                        method.hasAnnotation(OnMessage::class.java.name)
                    }.map { method ->
                        val loadedMethod = method.loadClassAndGetMethod()
                        MessageListener(
                            clazzInstance,
                            loadedMethod
                        )
                    }
                }

                pollAnswerListeners = classes.flatMap {
                    val clazz = it.loadClass().getDeclaredConstructor()
                    clazz.trySetAccessible()
                    val clazzInstance = clazz.newInstance()
                    it.methodAndConstructorInfo.filter { method ->
                        method.hasAnnotation(OnPollAnswer::class.java.name)
                    }.map { method ->
                        val loadedMethod = method.loadClassAndGetMethod()
                        PollAnswerListener(
                            clazzInstance,
                            loadedMethod
                        )
                    }
                }

                pollListeners = classes.flatMap {
                    val clazz = it.loadClass().getDeclaredConstructor()
                    clazz.trySetAccessible()
                    val clazzInstance = clazz.newInstance()
                    it.methodAndConstructorInfo.filter { method ->
                        method.hasAnnotation(OnPoll::class.java.name)
                    }.map { method ->
                        val loadedMethod = method.loadClassAndGetMethod()
                        PollListener(
                            clazzInstance,
                            loadedMethod
                        )
                    }
                }
            }
    }
}