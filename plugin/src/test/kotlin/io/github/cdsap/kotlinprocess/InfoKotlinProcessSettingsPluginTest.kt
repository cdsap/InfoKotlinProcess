package io.github.cdsap.kotlinprocess

import junit.framework.TestCase.assertTrue
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class InfoKotlinProcessSettingsPluginTest {
    @Rule
    @JvmField
    val testProjectDir = TemporaryFolder()

    @Test
    fun kotlinSettingsPluginCoversAllProjects() {
        writeKotlinMultiProject()

        assertOutput(runBuild().output)
    }

    @Test
    fun groovySettingsPluginCoversAllProjects() {
        writeGroovyMultiProject()

        assertOutput(runBuild().output)
    }

    @Test
    fun kotlinSettingsPluginSupportsConfigurationCacheReuse() {
        // Keep Kotlin off this build: TestKit settings classpath + Kotlin 2.0 + CC hits a
        // BuildFusService classloader clash. CC reuse covers settings-plugin registration;
        // process output is asserted by the multi-project tests.
        testProjectDir.newFile("settings.gradle.kts").writeText(
            """
            plugins {
                id("io.github.cdsap.kotlinprocess")
            }
            rootProject.name = "settings-cc-test"
            """.trimIndent(),
        )
        testProjectDir.newFile("build.gradle.kts").writeText(
            """
            tasks.register("probe") {
                doLast { println("probe-ok") }
            }
            """.trimIndent(),
        )

        val ccArguments =
            listOf(
                "probe",
                "--configuration-cache",
                "--configuration-cache-problems=fail",
            )
        val firstBuild = runBuild(ccArguments)
        val secondBuild = runBuild(ccArguments)

        assertTrue(firstBuild.output.contains("Configuration cache entry stored"))
        assertTrue(secondBuild.output.contains("Configuration cache entry reused."))
        assertTrue(secondBuild.output.contains("probe-ok"))
    }

    @Test
    fun projectPluginCompatibilityPathStillReports() {
        testProjectDir.newFile("settings.gradle.kts").writeText(
            """
            rootProject.name = "project-plugin-compat"
            """.trimIndent(),
        )
        testProjectDir.newFile("build.gradle.kts").writeText(
            """
            plugins {
                id("org.jetbrains.kotlin.jvm") version "2.0.20"
                id("io.github.cdsap.kotlinprocess")
            }
            repositories {
                mavenCentral()
            }
            """.trimIndent(),
        )
        testProjectDir.newFolder("src/main/kotlin/com/example")
        testProjectDir.newFile("src/main/kotlin/com/example/Hello.kt").writeText(
            """
            package com.example
            class Hello
            """.trimIndent(),
        )

        assertOutput(runBuild().output)
    }

    @Test
    fun settingsPluginAcceptsGbosOptInDsl() {
        testProjectDir.newFile("settings.gradle.kts").writeText(
            """
            plugins {
                id("io.github.cdsap.kotlinprocess")
            }
            infoKotlinProcess {
                gbos {
                    develocity.set(true)
                }
            }
            if (!infoKotlinProcess.gbos.develocity.get()) {
                throw GradleException("GBOS Develocity opt-in was not applied from settings")
            }
            rootProject.name = "settings-gbos-opt-in"
            """.trimIndent(),
        )
        testProjectDir.newFile("build.gradle.kts").writeText(
            """
            tasks.register("assertGbosOptIn") {
                doLast {
                    val extension =
                        project.extensions.getByName("infoKotlinProcess")
                            as io.github.cdsap.kotlinprocess.InfoKotlinProcessExtension
                    if (!extension.gbos.develocity.get()) {
                        throw GradleException("GBOS Develocity opt-in was not mirrored to the root project")
                    }
                }
            }
            """.trimIndent(),
        )

        val build = runBuild(listOf("assertGbosOptIn"))
        assertTrue(build.output.contains("BUILD SUCCESSFUL"))
    }

    private fun writeKotlinMultiProject() {
        testProjectDir.newFile("settings.gradle.kts").writeText(
            """
            plugins {
                id("io.github.cdsap.kotlinprocess")
            }
            rootProject.name = "settings-plugin-test"
            include(":app")
            """.trimIndent(),
        )
        testProjectDir.newFile("build.gradle.kts").writeText("")
        writeKotlinSubproject("build.gradle.kts")
    }

    private fun writeGroovyMultiProject() {
        testProjectDir.newFile("settings.gradle").writeText(
            """
            plugins {
                id 'io.github.cdsap.kotlinprocess'
            }
            rootProject.name = 'settings-plugin-test'
            include ':app'
            """.trimIndent(),
        )
        testProjectDir.newFile("build.gradle").writeText("")
        writeKotlinSubproject("build.gradle")
    }

    private fun writeKotlinSubproject(buildFileName: String) {
        testProjectDir.newFolder("app")
        val kotlinPlugin =
            if (buildFileName.endsWith(".gradle")) {
                "id 'org.jetbrains.kotlin.jvm' version '2.0.20'"
            } else {
                "id(\"org.jetbrains.kotlin.jvm\") version \"2.0.20\""
            }
        testProjectDir.newFile("app/$buildFileName").writeText(
            """
            plugins {
                $kotlinPlugin
            }
            repositories {
                mavenCentral()
            }
            """.trimIndent(),
        )
        testProjectDir.newFolder("app/src/main/kotlin/com/example")
        testProjectDir.newFile("app/src/main/kotlin/com/example/Hello.kt").writeText(
            """
            package com.example
            class Hello
            """.trimIndent(),
        )
    }

    private fun runBuild(arguments: List<String> = listOf("compileKotlin", "--info")) =
        GradleRunner
            .create()
            .withProjectDir(testProjectDir.root)
            .withArguments(arguments)
            .withPluginClasspath()
            .withGradleVersion("8.14.2")
            .build()

    private fun assertOutput(output: String) {
        assertTrue(
            "Expected settings plugin to report Kotlin processes, got:\n$output",
            output.contains("Kotlin processes"),
        )
        assertTrue(output.contains("PID"))
    }
}
