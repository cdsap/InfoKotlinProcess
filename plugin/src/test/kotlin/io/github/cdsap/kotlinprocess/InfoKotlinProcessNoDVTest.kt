package io.github.cdsap.kotlinprocess

import junit.framework.TestCase
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class InfoKotlinProcessNoDVTest {
    @Rule
    @JvmField
    val testProjectDir = TemporaryFolder()

    @Test
    fun gbosDevelocityOptInDefaultsOffAndCanBeEnabledInDsl() {
        testProjectDir.newFile("settings.gradle").writeText("")
        testProjectDir.newFile("build.gradle").writeText(
            """
            plugins {
                id 'io.github.cdsap.kotlinprocess'
            }

            if (infoKotlinProcess.gbos.develocity.get()) {
                throw new GradleException('GBOS Develocity reporting should default to off')
            }

            infoKotlinProcess {
                gbos {
                    develocity.set(true)
                }
            }

            tasks.register('assertGbosOptIn') {
                inputs.property('gbosEnabled', infoKotlinProcess.gbos.develocity)
                doLast {
                    if (inputs.properties['gbosEnabled'] != true) {
                        throw new GradleException('GBOS Develocity opt-in was not applied')
                    }
                }
            }
            """.trimIndent(),
        )

        val build =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir.root)
                .withArguments("assertGbosOptIn", "--configuration-cache", "--configuration-cache-problems=fail")
                .withPluginClasspath()
                .withGradleVersion("8.14.2")
                .build()

        TestCase.assertTrue(build.output.contains("BUILD SUCCESSFUL"))
    }

    @Test
    fun testPluginIsCompatibleWithConfigurationCacheWithDevelocity() {
        testProjectDir.newFile("settings.gradle").appendText(
            """
            """.trimIndent(),
        )
        testProjectDir.newFile("build.gradle").appendText(
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
        testProjectDir.newFolder("src/main/kotlin/com/example")
        testProjectDir.newFile("src/main/kotlin/com/example/Hello.kt").appendText(
            """
            package com.example
            class Hello() {
              fun print() {
                println("hello")
              }
            }
            """.trimIndent(),
        )
        listOf("8.14.2").forEach {
            val configurationCacheArgs =
                listOf(
                    "compileKotlin",
                    "--configuration-cache",
                    "--configuration-cache-problems=fail",
                )
            val firstBuild =
                GradleRunner
                    .create()
                    .withProjectDir(testProjectDir.root)
                    .withArguments(configurationCacheArgs)
                    .withPluginClasspath()
                    .withGradleVersion(it)
                    .build()
            val secondBuild =
                GradleRunner
                    .create()
                    .withProjectDir(testProjectDir.root)
                    .withArguments(configurationCacheArgs)
                    .withPluginClasspath()
                    .withGradleVersion(it)
                    .build()
            TestCase.assertTrue(
                "Expected configuration cache store on first run for Gradle $it, got:\n${firstBuild.output}",
                firstBuild.output.contains("Configuration cache entry stored"),
            )
            TestCase.assertTrue(
                "Expected configuration cache HIT on second run for Gradle $it, got:\n${secondBuild.output}",
                secondBuild.output.contains("Configuration cache entry reused."),
            )
        }
    }

    @Test
    fun testPluginIsCompatibleWithProjectIsolation() {
        testProjectDir.newFile("settings.gradle").appendText(
            """
            """.trimIndent(),
        )
        testProjectDir.newFile("build.gradle").appendText(
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
        listOf("8.14.2").forEach {
            val firstBuild =
                GradleRunner
                    .create()
                    .withProjectDir(testProjectDir.root)
                    .withArguments("compileKotlin", "-Dorg.gradle.unsafe.isolated-projects=true")
                    .withPluginClasspath()
                    .withGradleVersion(it)
                    .build()
            val secondBuild =
                GradleRunner
                    .create()
                    .withProjectDir(testProjectDir.root)
                    .withArguments("compileKotlin", "-Dorg.gradle.unsafe.isolated-projects=true")
                    .withPluginClasspath()
                    .withGradleVersion(it)
                    .build()
            TestCase.assertTrue(firstBuild.output.contains("Configuration cache entry stored"))
            TestCase.assertTrue(secondBuild.output.contains("Configuration cache entry reused."))
        }
    }
}
