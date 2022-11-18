package io.github.cdsap.kotlinprocess

import junit.framework.TestCase.assertTrue
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class InfoKotlinProcessPluginTest {
    @Rule
    @JvmField
    val testProjectDir = TemporaryFolder()

    @Test
    fun testOutputIsGeneratedWhenPluginIsApplied() {
        testProjectDir.newFile("build.gradle").appendText(
            """
                plugins {
                    id 'org.jetbrains.kotlin.jvm' version '1.7.21'
                    id 'application'
                    id 'io.github.cdsap.kotlinprocess'
                }
            """.trimIndent()
        )
        val build = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("compileKotlin", "--info")
            .withPluginClasspath()
            .withGradleVersion("7.5.1")
            .build()
        assertTrue(build.output.contains("Kotlin processes"))
        assertTrue(build.output.contains("PID"))
        assertTrue(build.output.contains("Capacity"))
        assertTrue(build.output.contains("Uptime"))
    }
}
