package io.github.cdsap.kotlinprocess

import junit.framework.TestCase.assertTrue
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class InfoKotlinProcessPluginTest {

    private val gradleVersions = listOf("7.5.1", "7.6", "8.0.1")

    @Rule
    @JvmField
    val testProjectDir = TemporaryFolder()

    @Test
    fun testOutputIsGeneratedWhenPluginIsApplied() {
        createBuildGradle()
        createKotlinClass()

        gradleVersions.forEach {
            val build = simpleKotlinCompileBuild(it)
            assertTerminalOutput(build)
        }
    }


    @Test
    fun testPluginIsCompatibleWithConfigurationCacheWithoutGradleEnterprise() {
        createBuildGradle()

       gradleVersions.forEach {
            val firstBuild = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("compileKotlin", "--configuration-cache")
                .withPluginClasspath()
                .withGradleVersion(it)
                .build()
            val secondBuild = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("compileKotlin", "--configuration-cache")
                .withPluginClasspath()
                .withGradleVersion(it)
                .build()

            assertTrue(firstBuild.output.contains("Configuration cache entry stored"))
            assertTrue(secondBuild.output.contains("Configuration cache entry reused."))
        }
    }

    @Test
    fun testOutputIsGeneratedWhenPluginIsAppliedWithJvmArgs() {
        testProjectDir.newFile("gradle.properties").appendText("""
            org.gradle.jvmargs=-Xmx256m -Dfile.encoding=UTF-8
        """.trimIndent())
        createBuildGradle()
        createKotlinClass()

        gradleVersions.forEach {
            val build = simpleKotlinCompileBuild(it)
            assertTerminalOutput(build)
        }
    }

    @Test
    fun testOutputIsGeneratedWhenPluginIsAppliedWithJvmArgsAndKotlinJvm() {
        testProjectDir.newFile("gradle.properties").appendText("""
            org.gradle.jvmargs=-Xmx256m -Dfile.encoding=UTF-8
            kotlin.daemon.jvmargs=-Xmx128m
        """.trimIndent())
        createBuildGradle()
        createKotlinClass()

        gradleVersions.forEach {
            val build = simpleKotlinCompileBuild(it)
            assertTerminalOutput(build)
        }
    }

    @Test
    fun testOutputIsGeneratedWhenPluginIsAppliedWithJvmArgsAndKotlinGCJvm() {
        testProjectDir.newFile("gradle.properties").appendText("""
            org.gradle.jvmargs=-Xmx256m -Dfile.encoding=UTF-8
            kotlin.daemon.jvmargs=-Xmx128m -XX:+UseParallelGC
        """.trimIndent())
        createBuildGradle()
        createKotlinClass()

        gradleVersions.forEach {
            val build = simpleKotlinCompileBuild(it)
            assertTerminalOutput(build)
        }
    }

    @Test
    fun testOutputIsGeneratedWhenPluginIsAppliedWithJvmGCArgsAndKotlinJvm() {
        testProjectDir.newFile("gradle.properties").appendText("""
            org.gradle.jvmargs=-Xmx256m -XX:+UseParallelGC -Dfile.encoding=UTF-8
        """.trimIndent())
        createBuildGradle()
        createKotlinClass()

        gradleVersions.forEach {
            val build = simpleKotlinCompileBuild(it)
            assertTerminalOutput(build)
        }
    }

    @Test
    fun testOutputIsGeneratedWhenPluginIsAppliedWithJvmGCArgsAndKotlinGCJvm() {
        testProjectDir.newFile("gradle.properties").appendText("""
            org.gradle.jvmargs=-Xmx256m -XX:+UseParallelGC -Dfile.encoding=UTF-8
            kotlin.daemon.jvmargs=-Xmx128m -XX:+UseParallelGC
        """.trimIndent())
        createBuildGradle()
        createKotlinClass()

        gradleVersions.forEach {
            val build = simpleKotlinCompileBuild(it)
            assertTerminalOutput(build)
        }
    }

    private fun simpleKotlinCompileBuild(it: String): BuildResult = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withArguments("compileKotlin", "--info")
        .withPluginClasspath()
        .withGradleVersion(it)
        .withDebug(true)
        .build()

    private fun assertTerminalOutput(build: BuildResult) {
        print(build.output)
        assertTrue(build.output.contains("Kotlin processes"))
        assertTrue(build.output.contains("PID"))
        assertTrue(build.output.contains("Capacity"))
        assertTrue(build.output.contains("Uptime"))
        assertTrue(build.output.contains("minutes"))
        assertTrue(build.output.contains("Gb"))
    }

    private fun createBuildGradle() {
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
    }

    private fun createKotlinClass() {
        testProjectDir.newFolder("src/main/kotlin/com/example")
        testProjectDir.newFile("src/main/kotlin/com/example/Hello.kt").appendText(
            """
                    package com.example
                    class Hello() {
                      fun print() {
                        println("hello")
                      }
                    }
                    """.trimIndent()
        )
    }
}
