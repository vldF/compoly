package modules.chatbot

import io.github.classgraph.ClassGraph
import log
import modules.chatbot.chatBotEvents.LongPollEventBase
import modules.chatbot.chatBotEvents.LongPollNewMessageEvent
import modules.chatbot.listeners.CommandListener
import modules.chatbot.listeners.MessageListener
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors

class EventProcessor(private val queue: ConcurrentLinkedQueue<LongPollEventBase>) : Thread() {
    private val pollSize = 4
    private val poll = Executors.newFixedThreadPool(pollSize)

    companion object {
        lateinit var commandListeners: List<CommandListener>
        lateinit var messageListeners: List<MessageListener>
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

    private fun process(event: LongPollEventBase) {
        when (event) {
            is LongPollNewMessageEvent -> {
                //messageListeners.forEach { it.call.invoke(it.baseClass, event) }

                val text = event.text
                if (text.isEmpty()) return
                log.info("new message: $text")

                val commandName = text.split(" ")[0].removePrefix("/")
                for (module in commandListeners) {
                    if (module.commands.contains(commandName)) {
                        //todo: add purchasing and permissions
                        module.call.invoke(module.baseClass, event)
                        log.info("command: $text")
                    }
                }
            }

        }
    }

    override fun run() {
        log.info("initializing modules")
        initModules()
        log.info("Starting event processor")
        mainLoop()
    }

    private fun initModules() {
        ClassGraph().enableAllInfo().whitelistPackages("modules.chatbot.chatModules")
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
                                            annotation.cost
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
                }
    }
}