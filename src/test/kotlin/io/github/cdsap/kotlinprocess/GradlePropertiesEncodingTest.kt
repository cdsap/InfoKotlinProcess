package io.github.cdsap.kotlinprocess

import junit.framework.TestCase.assertTrue
import org.junit.Test
import java.io.File
import java.util.Properties

class GradlePropertiesEncodingTest {
    @Test
    fun gradlePropertiesPinsUtf8FileEncoding() {
        val gradlePropertiesFile = File("gradle.properties")
        assertTrue(
            "Expected gradle.properties at project root to pin file encoding",
            gradlePropertiesFile.exists(),
        )

        val properties =
            Properties().apply {
                gradlePropertiesFile.inputStream().use { load(it) }
            }
        val jvmArgs = properties.getProperty("org.gradle.jvmargs").orEmpty()
        assertTrue(
            "Expected org.gradle.jvmargs to include -Dfile.encoding=UTF-8, got: $jvmArgs",
            jvmArgs.split(Regex("\\s+")).contains("-Dfile.encoding=UTF-8"),
        )
    }
}
