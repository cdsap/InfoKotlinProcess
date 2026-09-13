package io.github.cdsap.kotlinprocess

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Regression guard for keeping sources out of the root project
 * (Gradle best practice: no source in the root project).
 */
class ProjectStructureTest {
    @Test
    fun sourcesLiveInPluginSubprojectNotRoot() {
        val repoRoot = locateRepoRoot()

        assertFalse(
            "Root project must not contain src/ (sources belong in plugin/)",
            File(repoRoot, "src").exists(),
        )
        assertTrue(
            "Expected plugin production sources under plugin/src/main/kotlin",
            File(repoRoot, "plugin/src/main/kotlin").isDirectory,
        )
        assertTrue(
            "Expected plugin test sources under plugin/src/test/kotlin",
            File(repoRoot, "plugin/src/test/kotlin").isDirectory,
        )
        assertTrue(
            "Expected plugin build script at plugin/build.gradle.kts",
            File(repoRoot, "plugin/build.gradle.kts").isFile,
        )
    }

    @Test
    fun publishedProjectNameRemainsInfokotlinprocess() {
        assertEquals(
            "Publication coordinates depend on project.name remaining infokotlinprocess",
            "infokotlinprocess",
            System.getProperty("infokotlinprocess.project.name"),
        )
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
