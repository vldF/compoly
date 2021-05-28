package base.utils

import java.io.File
import java.net.URL
import java.net.URLClassLoader
import kotlin.streams.toList

fun getFileComparisonThrowable(
    message: String,
    expected: String,
    actual: String,
    expectedFilePath: String
): Throwable? {
    val rtLib = getIdeaRTLib() ?: return null
    val constructor = rtLib.constructors.first{ it.parameterCount == 4 }
    // String message, String expected, String actual, String expectedFilePath
    return constructor?.newInstance(message, expected, actual, expectedFilePath) as Throwable
}

private fun getIdeaRTLib(): Class<*>? {
    val sep = System.getProperty("file.separator")
    val ideaLibPath = getIdeaLibraryPath() ?: return null
    val rtPath = ideaLibPath + sep + "idea_rt.jar"
    val file = File(rtPath)
    val child = URLClassLoader(arrayOf<URL>(file.toURI().toURL()), object {}::class.java.classLoader)
    return Class.forName("com.intellij.rt.execution.junit.FileComparisonFailure", true, child)
}

private fun getIdeaLibraryPath(): String? {
    val ideaProcess = ProcessHandle.allProcesses()
        .toList()
        .firstOrNull { process ->
            process.info().arguments().isPresent && process.info().arguments().get().lastOrNull() == "com.intellij.idea.Main"
        }
        ?: return null
    val classpath = ideaProcess.info().arguments().get()[1] ?: return null

    return classpath
        .split(":")
        .firstOrNull { it.contains("bootstrap.jar") }
        ?.removeSuffix("bootstrap.jar")
}
