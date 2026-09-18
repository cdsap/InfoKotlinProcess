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
