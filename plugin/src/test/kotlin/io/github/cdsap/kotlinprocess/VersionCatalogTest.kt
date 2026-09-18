package io.github.cdsap.kotlinprocess

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class VersionCatalogTest {
    @Test
    fun versionCatalogCentralizesPluginAndLibraryVersions() {
        val catalog = locateProjectFile("gradle/libs.versions.toml")
        assertTrue(
            "Expected gradle/libs.versions.toml at ${catalog.absolutePath}",
            catalog.isFile,
        )

        val catalogText = catalog.readText()
        listOf(
            "jdkToolsParser",
            "commandlineValueSource",
            "picnic",
            "develocity",
            "junit",
            "cdsap-jdkToolsParser",
            "cdsap-commandlineValueSource",
            "develocity-gradlePlugin",
            "pluginPublish",
            "ktlint",
            "io.github.cdsap:jdk-tools-parser",
            "io.github.cdsap:commandline-value-source",
            "com.jakewharton.picnic:picnic",
            "com.gradle:develocity-gradle-plugin",
            "junit:junit",
            "com.gradle.plugin-publish",
            "org.jlleitschuh.gradle.ktlint",
        ).forEach { expected ->
            assertTrue(
                "gradle/libs.versions.toml must declare $expected",
                catalogText.contains(expected),
            )
        }

        val buildScript = locateProjectFile("plugin/build.gradle.kts")
        assertTrue(
            "Expected plugin/build.gradle.kts at ${buildScript.absolutePath}",
            buildScript.isFile,
        )

        val buildText = buildScript.readText()
        listOf(
            "alias(libs.plugins.pluginPublish)",
            "alias(libs.plugins.ktlint)",
            "implementation(libs.cdsap.jdkToolsParser)",
            "implementation(libs.cdsap.commandlineValueSource)",
            "implementation(libs.picnic)",
            "compileOnly(libs.develocity.gradlePlugin)",
            "testImplementation(libs.junit)",
        ).forEach { expected ->
            assertTrue(
                "plugin/build.gradle.kts must consume the version catalog via $expected",
                buildText.contains(expected),
            )
        }

        listOf(
            """id("com.gradle.plugin-publish") version """,
            """id("org.jlleitschuh.gradle.ktlint") version """,
            "io.github.cdsap:jdk-tools-parser:",
            "io.github.cdsap:commandline-value-source:",
            "com.jakewharton.picnic:picnic:",
            "com.gradle:develocity-gradle-plugin:",
            "junit:junit:",
        ).forEach { forbidden ->
            assertFalse(
                "plugin/build.gradle.kts must not hardcode dependency/plugin coordinates: $forbidden",
                buildText.contains(forbidden),
            )
        }
    }

    private fun locateProjectFile(relativePath: String): File {
        var directory = File(System.getProperty("user.dir")).canonicalFile
        while (true) {
            val candidate = File(directory, relativePath)
            if (candidate.isFile) {
                return candidate
            }
            val parent = directory.parentFile ?: break
            directory = parent
        }
        return File(relativePath)
    }
}
