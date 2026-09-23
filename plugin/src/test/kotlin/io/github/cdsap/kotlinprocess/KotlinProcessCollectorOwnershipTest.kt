package io.github.cdsap.kotlinprocess

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Regression guard for keeping ConsolidateProcesses / TypeProcess.Kotlin
 * owned by KotlinProcessCollector rather than reporting adapters.
 */
class KotlinProcessCollectorOwnershipTest {
    @Test
    fun consolidateProcessesAndKotlinTypeAreOwnedOnlyByCollector() {
        val mainKotlinSources = locateMainKotlinSources()

        assertEquals(
            "ConsolidateProcesses must live only in KotlinProcessCollector",
            listOf("KotlinProcessCollector.kt"),
            mainKotlinSources
                .filter { it.readText().contains("ConsolidateProcesses") }
                .map { it.name }
                .sorted(),
        )
        assertEquals(
            "TypeProcess.Kotlin must be selected only in KotlinProcessCollector",
            listOf("KotlinProcessCollector.kt"),
            mainKotlinSources
                .filter { it.readText().contains("TypeProcess.Kotlin") }
                .map { it.name }
                .sorted(),
        )
    }

    @Test
    fun reportingAdaptersUseSharedProviderMaterializationHelper() {
        val consolidation =
            locateMainKotlinSources()
                .single { it.name == "KotlinProcessConsolidation.kt" }
                .readText()
        val buildService =
            locateMainKotlinSources()
                .single { it.name == "InfoKotlinProcessBuildService.kt" }
                .readText()
        val develocity =
            locateMainKotlinSources()
                .single { it.name == "DevelocityWrapperConfiguration.kt" }
                .readText()

        assertTrue(
            "KotlinProcessConsolidation must materialize providers via KotlinProcessCollector",
            consolidation.contains("KotlinProcessCollector().collect("),
        )
        assertTrue(
            "InfoKotlinProcessBuildService must use collectKotlinProcesses",
            buildService.contains("collectKotlinProcesses("),
        )
        assertTrue(
            "DevelocityWrapperConfiguration must use collectKotlinProcesses",
            develocity.contains("collectKotlinProcesses("),
        )
        assertFalse(
            "InfoKotlinProcessBuildService must not call KotlinProcessCollector directly",
            buildService.contains("KotlinProcessCollector"),
        )
        assertFalse(
            "DevelocityWrapperConfiguration must not call KotlinProcessCollector directly",
            develocity.contains("KotlinProcessCollector"),
        )
        assertFalse(
            "InfoKotlinProcessBuildService must not call ConsolidateProcesses directly",
            buildService.contains("ConsolidateProcesses"),
        )
        assertFalse(
            "DevelocityWrapperConfiguration must not call ConsolidateProcesses directly",
            develocity.contains("ConsolidateProcesses"),
        )
    }

    private fun locateMainKotlinSources(): List<File> {
        val mainKotlin = File(locateRepoRoot(), "plugin/src/main/kotlin")
        assertTrue("Expected plugin production sources under plugin/src/main/kotlin", mainKotlin.isDirectory)
        return mainKotlin
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()
    }

    private fun locateRepoRoot(): File {
        var directory = File(System.getProperty("user.dir")).canonicalFile
        while (true) {
            val settings = File(directory, "settings.gradle.kts")
            val wrapper = File(directory, "gradle/wrapper/gradle-wrapper.properties")
            if (settings.isFile && wrapper.isFile) {
                return directory
            }
            val parent = directory.parentFile ?: break
            directory = parent
        }
        error("Could not locate repository root from ${System.getProperty("user.dir")}")
    }
}
