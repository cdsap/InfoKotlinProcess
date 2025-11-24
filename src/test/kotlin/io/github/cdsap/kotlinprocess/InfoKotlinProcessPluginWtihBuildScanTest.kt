package io.github.cdsap.kotlinprocess

import junit.framework.TestCase
import junit.framework.TestCase.assertTrue
import org.gradle.testkit.runner.GradleRunner
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class InfoKotlinProcessPluginWtihBuildScanTest {

    @Rule
    @JvmField
    val testProjectDir = TemporaryFolder()

    @Test
    fun testPluginIsCompatibleWithConfigurationCacheWithDevelocity() {
        Assume.assumeTrue(
            "Gradle Enterprise URL and Access Key are set",
            System.getenv("GE_URL") != null && System.getenv("GE_API_KEY") != null
        )

        testProjectDir.newFile("settings.gradle").appendText(
            """
                plugins {
                    id 'com.gradle.develocity' version '2.2.20'
                }
                develocity {
                    server = "${System.getenv("GE_URL")}"
                    accessKey="${System.getenv("GE_API_KEY")}"
                    buildScan {
                        publishing { true }
                    }
                }
            """.trimIndent()
        )
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
        listOf("8.14.2","9.2.1").forEach {
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
    fun testPluginIsCompatibleWithProjectIsolationWithDevelocity() {
        Assume.assumeTrue(
            "Gradle Enterprise URL and Access Key are set",
            System.getenv("GE_URL") != null && System.getenv("GE_API_KEY") != null
        )

        testProjectDir.newFile("settings.gradle").appendText(
            """
                plugins {
                    id 'com.gradle.develocity' version '4.2'
                }
                develocity {
                    server = "${System.getenv("GE_URL")}"
                    accessKey="${System.getenv("GE_API_KEY")}"
                    buildScan {
                        publishing { true }
                    }
                }
            """.trimIndent()
        )
        testProjectDir.newFile("build.gradle").appendText(
            """
                plugins {
                    id 'org.jetbrains.kotlin.jvm' version '2.2.20'
                    id 'application'
                    id 'io.github.cdsap.kotlinprocess'
                }
                repositories {
                    mavenCentral()
                }

            """.trimIndent()
        )
        listOf("8.14.2", "9.2.1").forEach {
            val firstBuild = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("compileKotlin", "-Dorg.gradle.unsafe.isolated-projects=true")
                .withPluginClasspath()
                .withGradleVersion(it)
                .build()
            val secondBuild = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("compileKotlin", "-Dorg.gradle.unsafe.isolated-projects=true")
                .withPluginClasspath()
                .withGradleVersion(it)
                .build()
            println(firstBuild.output)
            println(secondBuild.output)
            assertTrue(firstBuild.output.contains("Configuration cache entry stored"))
            assertTrue(secondBuild.output.contains("Configuration cache entry reused."))
        }
    }
}
