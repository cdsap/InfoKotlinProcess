package io.github.cdsap.kotlinprocess

import junit.framework.TestCase.assertTrue
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Regression for classpath-vs-applied mismatch: Develocity types may be visible on the
 * plugin classpath (e.g. transitively) without `com.gradle.develocity` being applied.
 * Console reporting must still run in that case.
 *
 * Also covers settings-applied Develocity: even when Build Scan wiring cannot share
 * types across TestKit classloaders, the build must not silently produce no output.
 */
class InfoKotlinProcessClasspathProbeTest {
    @Rule
    @JvmField
    val testProjectDir = TemporaryFolder()

    @Test
    fun testConsoleOutputWhenDevelocityIsOnClasspathButNotApplied() {
        testProjectDir.newFile("build.gradle").writeText(
            """
            plugins {
                id 'org.jetbrains.kotlin.jvm' version '2.0.20'
                id 'application'
                id 'io.github.cdsap.kotlinprocess'
            }
            repositories {
                mavenCentral()
            }

            """.trimIndent(),
        )
        createKotlinClass()

        val develocityJar =
            File(
                System.getProperty("develocity.probe.classpath.jar")
                    ?: error("Missing develocity.probe.classpath.jar system property"),
            )
        val pluginClasspath =
            GradleRunner
                .create()
                .withPluginClasspath()
                .pluginClasspath + develocityJar

        val build =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir.root)
                .withArguments("compileKotlin", "--info")
                .withPluginClasspath(pluginClasspath)
                .withGradleVersion("8.14.1")
                .build()

        assertTrue(
            "Expected console Kotlin process output when Develocity is on the classpath " +
                "but not applied, got:\n${build.output}",
            build.output.contains("Kotlin processes"),
        )
    }

    @Test
    fun testSettingsAppliedDevelocityDoesNotSilentlySkipOutput() {
        testProjectDir.newFile("settings.gradle").writeText(
            """
            plugins {
                id 'com.gradle.develocity' version '4.5.1'
            }
            develocity {
                buildScan {
                    publishing.onlyIf { false }
                }
            }
            """.trimIndent(),
        )
        testProjectDir.newFile("build.gradle").writeText(
            """
            plugins {
                id 'org.jetbrains.kotlin.jvm' version '2.0.20'
                id 'application'
                id 'io.github.cdsap.kotlinprocess'
            }
            repositories {
                mavenCentral()
            }

            """.trimIndent(),
        )
        createKotlinClass()

        val build =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir.root)
                .withArguments("compileKotlin", "--info")
                .withPluginClasspath()
                .withGradleVersion("8.14.1")
                .build()

        assertTrue(
            "Settings-applied Develocity must not leave the plugin with neither scan nor " +
                "console output. Got:\n${build.output}",
            build.output.contains("Kotlin processes"),
        )
    }

    private fun createKotlinClass() {
        testProjectDir.newFolder("src/main/kotlin/com/example")
        testProjectDir.newFile("src/main/kotlin/com/example/Hello.kt").writeText(
            """
            package com.example
            class Hello() {
              fun print() {
                println("hello")
              }
            }
            """.trimIndent(),
        )
    }
}
