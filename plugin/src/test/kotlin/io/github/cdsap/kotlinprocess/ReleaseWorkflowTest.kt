package io.github.cdsap.kotlinprocess

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReleaseWorkflowTest {
    @Test
    fun releaseWorkflowPublishesOnlyTheMatchingNonSnapshotTagWithJdk17() {
        val workflow = locateRepoRoot().resolve(".github/workflows/release.yaml")
        assertTrue("Expected release workflow at ${workflow.absolutePath}", workflow.isFile)

        val content = workflow.readText()
        assertTrue("Release workflow must run for published releases", "types: [published]" in content)
        assertTrue(
            "Release workflow must check out the published release tag",
            "ref: \${{ github.event.release.tag_name }}" in content,
        )
        assertTrue("Release workflow must reject draft releases", "github.event.release.draft == false" in content)
        assertTrue("Release workflow must validate the tag and project version", "Release tag" in content)
        assertTrue(
            "Release workflow must reject snapshot versions",
            "Snapshot release tags cannot be published." in content,
        )
        assertTrue("Release workflow must fail when publish credentials are absent", "must be configured." in content)
        assertTrue(
            "Release workflow must validate the Gradle wrapper",
            "gradle/actions/wrapper-validation@v6" in content,
        )
        assertTrue(
            "Release workflow must use the Gradle setup and cache action",
            "gradle/actions/setup-gradle@v6" in content,
        )
        assertTrue("Release workflow must compile and publish with JDK 17", "java-version: 17" in content)
        assertTrue(
            "Release workflow must publish with publishPlugins",
            "run: ./gradlew --no-daemon publishPlugins" in content,
        )
        assertTrue(
            "Release workflow must pass the Gradle Portal key",
            "GRADLE_PUBLISH_KEY: \${{ secrets.GRADLE_PUBLISH_KEY }}" in content,
        )
        assertTrue(
            "Release workflow must pass the Gradle Portal secret",
            "GRADLE_PUBLISH_SECRET: \${{ secrets.GRADLE_PUBLISH_SECRET }}" in content,
        )
        assertFalse("The publication path must not use JDK 25", "java-version: 25" in content)
    }

    private fun locateRepoRoot(): File {
        var directory = File(System.getProperty("user.dir")).canonicalFile
        while (true) {
            if (File(directory, "settings.gradle.kts").isFile &&
                File(directory, "gradle/wrapper/gradle-wrapper.properties").isFile
            ) {
                return directory
            }
            directory = directory.parentFile ?: break
        }
        error("Could not locate repository root from ${System.getProperty("user.dir")}")
    }
}
