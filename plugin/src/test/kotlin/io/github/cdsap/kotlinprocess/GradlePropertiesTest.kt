package io.github.cdsap.kotlinprocess

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test
import java.io.File
import java.util.Properties

class GradlePropertiesTest {
    @Test
    fun buildCacheIsEnabledInRootGradleProperties() {
        val gradleProperties = locateRootGradleProperties()
        assertTrue(
            "Expected gradle.properties at ${gradleProperties.absolutePath}",
            gradleProperties.isFile,
        )

        val properties = Properties()
        gradleProperties.inputStream().use { properties.load(it) }
        assertEquals("true", properties.getProperty("org.gradle.caching"))
    }

    private fun locateRootGradleProperties(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null) {
            if (File(dir, "settings.gradle.kts").isFile) {
                return File(dir, "gradle.properties")
            }
            dir = dir.parentFile
        }
        return File("gradle.properties")
    }
}
