package modules.chatbot

import io.github.classgraph.ClassGraph
import log
import modules.chatbot.chatBotEvents.LongPollEventBase
import modules.chatbot.listeners.CommandListener
import modules.chatbot.listeners.MessageListener
import java.util.concurrent.ConcurrentLinkedQueue


object ChatBot: Thread() {
    private lateinit var commandListeners: List<CommandListener>
    private lateinit var messageListeners: List<MessageListener>

    override fun run() {
        log.info("Initializing ChatBot")
        log.info("Initializing modules")
        initModules()
        log.info("Initializing modules done")

        val queue = ConcurrentLinkedQueue<LongPollEventBase>()

        // longpolls
        VkLongPoll(queue).run()

        EventProcessor(queue).start()
    }

    fun initModules() {
        ClassGraph().enableAllInfo().whitelistPackages("modules.chatbot.chatModules")
                .scan().use { scanResult ->
                    val classes = scanResult.allClasses
                    commandListeners = classes.flatMap {
                        it.methodAndConstructorInfo.filter { method ->
                            method.hasAnnotation(OnCommand::class.java.name)
                        }.map { method ->
                            val loadedMethod = method.loadClassAndGetMethod()
                            val annotation = loadedMethod.getAnnotation(OnCommand::class.java)
                            CommandListener(
                                    annotation.commands,
                                    annotation.description,
                                    it.loadClass().getConstructor().newInstance(),
                                    loadedMethod,
                                    annotation.permissions,
                                    annotation.cost
                            )
                        }
                    }

                    messageListeners = classes.flatMap {
                        it.methodAndConstructorInfo.filter { method ->
                            method.hasAnnotation(OnMessage::class.java.name)
                        }.map { method ->
                            val loadedMethod = method.loadClassAndGetMethod()
                            MessageListener(
                                    it.loadClass().getConstructor().newInstance(),
                                    loadedMethod
                            )
                        }
                    }
                }
    }

}