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
                repositories {
                    mavenCentral()
                }
            """.trimIndent()
        )
        listOf("7.5.1", "7.6").forEach {
            val build = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("compileKotlin", "--info")
                .withPluginClasspath()
                .withGradleVersion(it)
                .build()
            assertTrue(build.output.contains("Kotlin processes"))
            assertTrue(build.output.contains("PID"))
            assertTrue(build.output.contains("Capacity"))
            assertTrue(build.output.contains("Uptime"))
        }
    }

    @Test
    fun testPluginIsCompatibleWithConfigurationCache() {
        testProjectDir.newFile("build.gradle").appendText(
            """
                plugins {
                    id 'org.jetbrains.kotlin.jvm' version '1.7.21'
                    id 'application'
                    id 'io.github.cdsap.kotlinprocess'
                }
                repositories {
                    mavenCentral()
                }

            """.trimIndent()
        )
        listOf("7.5.1", "7.6").forEach {
            val firstBuild = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("compileKotlin", "--configuration-cache", "--configuration-cache-problems=warn")
                .withPluginClasspath()
                .withGradleVersion(it)
                .build()
            val secondBuild = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("compileKotlin", "--configuration-cache", "--configuration-cache-problems=warn")
                .withPluginClasspath()
                .withGradleVersion(it)
                .build()
            assertTrue(firstBuild.output.contains("Configuration cache entry stored"))
            assertTrue(secondBuild.output.contains("Configuration cache entry reused."))
        }
    }
}
