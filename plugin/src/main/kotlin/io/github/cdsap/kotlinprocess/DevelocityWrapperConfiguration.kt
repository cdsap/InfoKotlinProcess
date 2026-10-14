package io.github.cdsap.kotlinprocess

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import io.github.cdsap.kotlinprocess.output.DevelocityValues
import io.github.cdsap.kotlinprocess.output.GbosOutputReport
import org.gradle.api.Project
import org.gradle.api.provider.Provider

class DevelocityWrapperConfiguration {
    /** @return true when Build Scan reporting was registered from the Develocity extension. */
    fun configureProjectWithDevelocityIfPresent(target: Project): Boolean =
        configureProjectWithDevelocityIfPresent(target, target.providers.provider { false }, target.providers.provider { false })

    fun configureProjectWithDevelocityIfPresent(
        target: Project,
        gbosJson: Provider<Boolean>,
        gbosNdjson: Provider<Boolean>,
    ): Boolean {
        val develocity = target.extensions.findByType(DevelocityConfiguration::class.java) ?: return false
        buildScanDevelocityReporting(target, develocity, gbosJson, gbosNdjson)
        return true
    }

    fun configureProjectWithDevelocity(target: Project) {
        configureProjectWithDevelocity(target, target.providers.provider { false }, target.providers.provider { false })
    }

    fun configureProjectWithDevelocity(
        target: Project,
        gbosJson: Provider<Boolean>,
        gbosNdjson: Provider<Boolean>,
    ) {
        val develocity = target.extensions.getByType(DevelocityConfiguration::class.java)
        buildScanDevelocityReporting(target, develocity, gbosJson, gbosNdjson)
    }

    private fun buildScanDevelocityReporting(
        project: Project,
        buildScanExtension: DevelocityConfiguration,
        gbosJson: Provider<Boolean>,
        gbosNdjson: Provider<Boolean>,
    ) {
        val providers = project.kotlinProcessProviders()
        val gbosJsonFile = project.layout.buildDirectory.file("info-kotlin-process/gbos.json")
        val gbosNdjsonFile = project.layout.buildDirectory.file("info-kotlin-process/gbos.ndjson")

        buildScanExtension.buildScan.buildFinished {
            val processes =
                providers.collect()
            DevelocityValues(buildScanExtension, processes, gbosJson.get() || gbosNdjson.get())
                .addProcessesInfoToBuildScan()
            GbosOutputReport(
                gbosJsonFile.get().asFile,
                gbosNdjsonFile.get().asFile,
            ).write(processes, gbosJson.get(), gbosNdjson.get())
        }
    }
}
