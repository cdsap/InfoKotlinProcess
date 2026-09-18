package io.github.cdsap.kotlinprocess

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SettingsRepositoriesTest {
    @Test
    fun settingsDoesNotDeclareGoogleRepository() {
        val settings = locateSettingsGradleKts().readText()
        assertFalse(
            "settings.gradle.kts must not declare google(); this JVM plugin has no Google-hosted dependencies",
            settings.contains(Regex("""\bgoogle\s*\(""")),
        )
    }

    @Test
    fun dependencyResolutionFiltersPluginPortalToComGradleGroups() {
        val settings = locateSettingsGradleKts().readText()
        val dependencyResolution =
            settings
                .substringAfter("dependencyResolutionManagement")
                .substringBefore("rootProject")

        assertTrue(
            "dependencyResolutionManagement must use exclusiveContent for the plugin portal",
            dependencyResolution.contains("exclusiveContent"),
        )
        assertTrue(
            "plugin portal exclusive content must be limited to com.gradle.* groups",
            dependencyResolution.contains("""includeGroupByRegex("com\\.gradle.*")""") ||
                dependencyResolution.contains("""includeGroupByRegex("com\\.gradle\\..*")"""),
        )
        assertTrue(
            "mavenCentral() must remain as the unfiltered default repository",
            dependencyResolution.contains("mavenCentral()"),
        )
        assertTrue(
            "gradlePluginPortal() must remain available for com.gradle coordinates",
            dependencyResolution.contains("gradlePluginPortal()"),
        )
    }

    @Test
    fun pluginManagementUsesOnlyPluginPortal() {
        val settings = locateSettingsGradleKts().readText()
        val pluginManagement =
            settings
                .substringAfter("pluginManagement")
                .substringBefore("dependencyResolutionManagement")

        assertTrue(
            "pluginManagement must declare gradlePluginPortal()",
            pluginManagement.contains("gradlePluginPortal()"),
        )
        assertFalse(
            "pluginManagement must not declare mavenCentral(); plugins resolve from the plugin portal",
            pluginManagement.contains("mavenCentral()"),
        )
        assertFalse(
            "pluginManagement must not declare google()",
            pluginManagement.contains(Regex("""\bgoogle\s*\(""")),
        )
    }

    private fun locateSettingsGradleKts(): File {
        var directory = File(System.getProperty("user.dir")).canonicalFile
        while (true) {
            val candidate = File(directory, "settings.gradle.kts")
            if (candidate.isFile) {
                return candidate
            }
            val parent = directory.parentFile ?: break
            directory = parent
        }
        return File("settings.gradle.kts")
    }
}
