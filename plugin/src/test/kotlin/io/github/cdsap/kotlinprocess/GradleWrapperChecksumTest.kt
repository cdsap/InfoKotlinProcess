package io.github.cdsap.kotlinprocess

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Properties

class GradleWrapperChecksumTest {
    @Test
    fun distributionSha256SumMatchesPublishedGradleChecksum() {
        val propertiesFile = locateWrapperProperties()
        assertTrue(
            "Expected gradle-wrapper.properties at ${propertiesFile.absolutePath}",
            propertiesFile.isFile,
        )

        val properties =
            Properties().apply {
                propertiesFile.inputStream().use { load(it) }
            }

        val distributionUrl = properties.getProperty("distributionUrl")
        assertEquals(
            "https://services.gradle.org/distributions/gradle-9.8.0-bin.zip",
            distributionUrl?.replace("\\:", ":"),
        )

        val distributionSha256Sum = properties.getProperty("distributionSha256Sum")
        assertTrue(
            "distributionSha256Sum must be set so the wrapper verifies the downloaded distribution",
            !distributionSha256Sum.isNullOrBlank(),
        )
        assertTrue(
            "distributionSha256Sum must be a 64-character hex digest",
            distributionSha256Sum.matches(Regex("[a-fA-F0-9]{64}")),
        )
        assertEquals(
            // Binary-only (-bin) ZIP checksum from https://gradle.org/release-checksums/
            "bafd5ce9cfaea0fbccfdc8439a1ac42fbd4cd9c89dc9a988228d8a2639a58e6c",
            distributionSha256Sum,
        )
    }

    private fun locateWrapperProperties(): File {
        var directory = File(System.getProperty("user.dir")).canonicalFile
        while (true) {
            val candidate = File(directory, "gradle/wrapper/gradle-wrapper.properties")
            if (candidate.isFile) {
                return candidate
            }
            val parent = directory.parentFile ?: break
            directory = parent
        }
        return File("gradle/wrapper/gradle-wrapper.properties")
    }
}
