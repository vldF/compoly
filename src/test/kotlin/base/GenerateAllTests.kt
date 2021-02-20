package base

import java.io.File

fun main() {
    val testDataDir = File("src/test/kotlin/testData")
    val testFiles = testDataDir.listFiles(File::isDirectory)?.sorted() ?: throw IllegalStateException("no tests were found")
    val code = buildString {
        appendln("package codegen")
        appendln()
        appendln("import base.runTest")
        appendln("import org.junit.jupiter.api.Test")
        appendln()
        appendln("//DO NOT MODIFY THIS FILE MANUALLY!!!")
        appendln()
        appendln("class TestsGenerated {")

        for (file in testFiles) {
            val testCode = getTestCode(file.name, file.path.normalizePath)
            appendln(testCode.lines().joinToString(separator = "\n") { "    $it" }) // adding spacing
        }
        appendln("}")
    }
    val codeFile = File("src/test/kotlin/codegen/TestsGenerated.kt")
    codeFile.writeText(code)
}

private fun getTestCode(testName: String, path: String): String {
    /*
    @Test
    fun TEST_NAME() {
        runTest(PATH)
    }
     */
    return buildString {
        appendln("@Test")
        appendln("fun $testName() {")
        appendln("    runTest(\"$path\")")
        appendln("}")
    }
}

private val String.normalizePath
    get() = this.replace("\\", "/")