package base

import chatbot.GenerateMock
import io.github.classgraph.ClassGraph
import io.github.classgraph.MethodInfoList
import krobot.api.kotlinFile
import krobot.api.t
import krobot.api.writeTo
import java.nio.file.Paths

fun main() {
    val apiMethods = ClassGraph().enableAllInfo().whitelistPackages("api")
            .scan().use { scanResult ->
                val apiObject = scanResult.allClasses.firstOrNull { it.name == "api.VkApi" }
                        ?: error("Api object not found")

                apiObject.methodAndConstructorInfo.filter { info ->
                    info.isPublic && info.hasAnnotation(GenerateMock::class.java.name)
                }
        }

    generateVkApiMockInterface(apiMethods)
    generateVkApiMockImplementation(apiMethods)
}


private fun generateVkApiMockInterface(apiMethods: MethodInfoList) {
    val imports = listOf(
        "api.VkApi",
        "api.keyboards.Keyboard",
        "api.objects.VkUser",
        "com.nhaarman.mockitokotlin2.*",
        "org.mockito.Answers",
        "org.mockito.invocation.InvocationOnMock",
        "org.mockito.stubbing.Answer"
    )

    val utilitiesFunctionsCode = """
        fun getMock(api: VkApiMock): VkApi {
            val answer = Answer {
                api.executeMockMethod(it) ?: Answers.RETURNS_DEFAULTS.answer(it)
            }
            return mock(defaultAnswer = answer)
        }

        private fun VkApiMock.executeMockMethod(invocation: InvocationOnMock): Any? {
            val method = this.javaClass.methods.find { it.name == invocation.method.name } 
            return if (method != null) {
                method.invoke(this, *invocation.arguments)
            } else {
                invocation.callRealMethod()
            }
        }
    """.trimIndent()

    val file = kotlinFile("base", imports = { imports.map { import(it) } }) {
        raw { writeln() }
        comment("DO NO MODIFY THIS CODE MANUALLY!!!")
        raw { writeln() }
        addInterface("VkApiMock") {
            for (methodInfo in apiMethods) {
                val arguments = methodInfo.parameterInfo
                val mockAnnotation = methodInfo.annotationInfo.first { it.name == GenerateMock::class.java.name }
                val argNames = (mockAnnotation.parameterValues[0].value as Array<*>).map { it as String }

                addAbstractFunction(
                    methodInfo.name,
                    parameters = {
                        for ((i, arg) in arguments.withIndex()) {
                            val type = arg.typeSignatureOrTypeDescriptor.toStringWithSimpleNames().kotlinNullableName
                            val name = argNames[i]

                            name of type
                        }
                    },
                    returnType = methodInfo.typeSignatureOrTypeDescriptor.resultType.toStringWithSimpleNames().kotlinNullableName.t
                )
                raw { writeln() }
            }
        }

        raw {
            writeln()
            writeln(utilitiesFunctionsCode)
        }

    }

    file.writeTo(Paths.get("./src/test/kotlin/base/VkApiMock.kt"))
}

private fun generateVkApiMockImplementation(apiMethods: MethodInfoList) {
    val imports = listOf(
        "api.keyboards.Keyboard",
        "api.objects.VkUser",
        "com.google.gson.Gson",
        "com.google.gson.JsonObject",
        "java.io.File"
    )

    val utilitiesFunctionsCode = """
    private inline fun <reified T> readValueFromFile(path: String, attributeName: String, defaultValue: T): T {
            val file = File("${'$'}path/${'$'}attributeName-in.txt")
            if (!file.exists()) return defaultValue
    
            val lines = file.readLines()
            if (lines.isEmpty()) return defaultValue
    
            val index = valuesReadCount.getOrPut(attributeName) { 0 } % lines.size
            valuesReadCount[attributeName] = index + 1
            val value = lines[index]
            return when(T::class) {
                Int::class -> value.toInt() as T
                Long::class -> value.toLong() as T
                String()::class -> value as T
                Boolean::class -> value.toBoolean() as T
                List::class -> {
                    tru {
                        Gson().fromJson(value, Array<VkUser>::class.java).toList() as T
                    } catch(e: Exception) {
                        throw IllegalStateException("wrong type parameter")
                    }
                }
                else -> throw IllegalStateException("wrong type")
            }
        }

        private fun writeResponse(methodName: String, vararg values: Pair<String, Any?>) {
            val json = JsonObject()
            for ((key, value) in values) {
                json.add(key, gson.toJsonTree(value))
            }
    
            keeper.write(methodName, json )
        }
    """.trimIndent()

    val file = kotlinFile("base", imports = { imports.map { import(it) } }) {
        raw { writeln() }
        comment("DO NO MODIFY THIS CODE MANUALLY!!!")
        raw { writeln() }
        addClass(
            "FiledVkApiMock",
            inheritance = { this.implement("VkApiMock".t) },
            primaryConstructor = {
                this@kotlinFile.raw{
                    writeln("private val pathToFile: String,")
                    writeln("private val keeper: ApiResponseKeeper")
                }
            }
        ) {
            raw {
                writeln("private val gson = Gson()")
                writeln("private val valuesReadCount = mutableMapOf<String, Int>()")
            }

            for (methodInfo in apiMethods) {
                val arguments = methodInfo.parameterInfo
                val mockAnnotation = methodInfo.annotationInfo.first { it.name == GenerateMock::class.java.name }
                val annotationParams = mockAnnotation.parameterValues
                val argNames = (annotationParams[0].value as Array<*>).map { it as String }
                val defaultValue = annotationParams[1].value as String
                val returnType = methodInfo.typeSignatureOrTypeDescriptor.resultType.toStringWithSimpleNames().kotlinNullableName
                val functionName = methodInfo.name

                addFunction(
                    functionName,
                    modifiers = { override() },
                    parameters = {
                        for ((i, arg) in arguments.withIndex()) {
                            val type = arg.typeSignatureOrTypeDescriptor.toStringWithSimpleNames().kotlinNullableName
                            val name = argNames[i]

                            name of type
                        }
                    },
                    returnType = returnType.t,
                    body = {
                        val values = argNames.joinToString { "\"$it\" to $it" }
                        raw {
                            writeln()
                            writeln("writeResponse(\"$functionName\", $values)")
                        }
                        if (returnType != "Unit") {
                            raw { writeln(
                                "return readValueFromFile(pathToFile, \"$functionName\", $defaultValue)"
                            ) }
                        }
                    }
                )
                raw { writeln() }
            }
            raw {
                writeln(utilitiesFunctionsCode)
            }
        }

    }

    file.writeTo(Paths.get("./src/test/kotlin/base/FiledVkApiMock.kt"))
}

private val String.kotlinNullableName: String
    get() {
        return when (this) {
            "int" -> "Int?"
            "boolean" -> "Boolean?"
            "void" -> "Unit"
            "long" -> "Long?"
            else -> "$this?"
        }
    }

