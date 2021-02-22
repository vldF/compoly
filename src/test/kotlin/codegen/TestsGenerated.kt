package codegen

import base.runTest
import base.afterTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

//DO NOT MODIFY THIS FILE MANUALLY!!!

class TestsGenerated {
    @AfterEach
        fun after() {
        afterTest()
    }
    @Test
    fun cats() {
        runTest("src/test/kotlin/testData/cats")
        afterTest()
    }
    
    @Test
    fun help() {
        runTest("src/test/kotlin/testData/help")
        afterTest()
    }
    
    @Test
    fun ratingSystem() {
        runTest("src/test/kotlin/testData/ratingSystem")
        afterTest()
    }
    
    @Test
    fun reward() {
        runTest("src/test/kotlin/testData/reward")
        afterTest()
    }
    
    @Test
    fun virtualTargets() {
        runTest("src/test/kotlin/testData/virtualTargets")
        afterTest()
    }
    
}
