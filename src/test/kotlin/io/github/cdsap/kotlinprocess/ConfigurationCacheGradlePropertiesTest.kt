package io.github.cdsap.kotlinprocess

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test
import java.io.File
import java.util.Properties

class ConfigurationCacheGradlePropertiesTest {
    @Test
    fun rootGradlePropertiesEnablesConfigurationCache() {
        val gradleProperties = File("gradle.properties")
        assertTrue(
            "Expected gradle.properties at the project root so the plugin build exercises " +
                "the configuration cache it declares in plugin metadata",
            gradleProperties.isFile,
        )

        val properties =
            Properties().apply {
                gradleProperties.inputStream().use { load(it) }
            }
        assertEquals(
            "true",
            properties.getProperty("org.gradle.configuration-cache"),
        )
    }
}
