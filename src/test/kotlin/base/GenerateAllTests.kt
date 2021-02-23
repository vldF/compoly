package base

import java.io.File

fun main() {
    val testDataDir = File("src/test/kotlin/testData")
    val testFiles = testDataDir.listFiles(File::isDirectory)?.sorted() ?: throw IllegalStateException("no tests were found")
    val code = buildString {
        appendln("package codegen")
        appendln()
        appendln("import base.runTest")
        appendln("import base.afterTest")
        appendln("import org.junit.jupiter.api.AfterEach")
        appendln("import org.junit.jupiter.api.Order")
        appendln("import org.junit.jupiter.api.Test")
        appendln()
        appendln("//DO NOT MODIFY THIS FILE MANUALLY!!!")
        appendln()
        appendln("class TestsGenerated {")
        appendln("    @AfterEach")
        appendln("    fun after() {")
        appendln("        afterTest()")
        appendln("    }")

        for ((order, file) in testFiles.withIndex()) {
            val testCode = getTestCode(file.name, file.path.normalizePath, order+1)
            appendln(testCode.lines().joinToString(separator = "\n") { "    $it" }) // adding spacing
        }
        appendln("}")
    }
    val codeFile = File("src/test/kotlin/codegen/TestsGenerated.kt")
    codeFile.writeText(code)
}

private fun getTestCode(testName: String, path: String, order: Int): String {
    /*
    @Test
    fun TEST_NAME() {
        runTest(PATH)
    }
     */
    return buildString {
        appendln("@Test")
        appendln("@Order($order)")
        appendln("fun $testName() {")
        appendln("    runTest(\"$path\")")
        appendln("}")
    }
}

private val String.normalizePath
    get() = this.replace("\\", "/")