package chatbot

import chatbot.chatBotEvents.LongPollEventBase
import chatbot.chatBotEvents.LongPollNewMessageEvent
import chatbot.listeners.CommandListener
import chatbot.listeners.MessageListener
import chatbot.listeners.PollAnswerListener
import chatbot.listeners.PollListener
import io.github.classgraph.ClassGraph
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
                        if (
                            module.permission == CommandPermission.USER
                            || module.permission <= Permissions.getUserPermissionsByNewMessageEvent(event)
                        ) {
                            try {
                                module.call.invoke(module.baseClass, event)
                                return
                            } catch (e: Exception) {
                                e.printStackTrace()
                                log.severe("Error on processing commandListeners")
                                log.info("command: $text")
                            }
                        }
                    }
                }
            }
        }
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
                            val annotation = loadedMethod.getAnnotation(OnCommand::class.java)
                            CommandListener(
                                annotation.commands,
                                annotation.description,
                                clazzInstance,
                                loadedMethod,
                                annotation.permissions,
                                annotation.showOnHelp
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