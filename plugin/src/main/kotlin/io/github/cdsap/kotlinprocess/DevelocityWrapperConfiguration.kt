package io.github.cdsap.kotlinprocess

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import io.github.cdsap.kotlinprocess.output.DevelocityValues
import io.github.cdsap.kotlinprocess.output.GbosOutputReport
import org.gradle.api.Project
import org.gradle.api.provider.Provider

class DevelocityWrapperConfiguration {
    /** @return true when Build Scan reporting was registered from the Develocity extension. */
    fun configureProjectWithDevelocityIfPresent(target: Project): Boolean =
        configureProjectWithDevelocityIfPresent(
            target,
            target.providers.provider { false },
            target.providers.provider { false },
        )

    fun configureProjectWithDevelocityIfPresent(
        target: Project,
        gbosEnabled: Provider<Boolean>,
    ): Boolean = configureProjectWithDevelocityIfPresent(target, gbosEnabled, gbosEnabled)

    fun configureProjectWithDevelocityIfPresent(
        target: Project,
        gbosEnabled: Provider<Boolean>,
        gbosDevelocityEnabled: Provider<Boolean>,
    ): Boolean {
        val develocity = target.extensions.findByType(DevelocityConfiguration::class.java) ?: return false
        buildScanDevelocityReporting(target, develocity, gbosEnabled, gbosDevelocityEnabled)
        return true
    }

    fun configureProjectWithDevelocity(target: Project) {
        configureProjectWithDevelocity(
            target,
            target.providers.provider { false },
            target.providers.provider { false },
        )
    }

    fun configureProjectWithDevelocity(
        target: Project,
        gbosEnabled: Provider<Boolean>,
    ) {
        configureProjectWithDevelocity(target, gbosEnabled, gbosEnabled)
    }

    fun configureProjectWithDevelocity(
        target: Project,
        gbosEnabled: Provider<Boolean>,
        gbosDevelocityEnabled: Provider<Boolean>,
    ) {
        val develocity = target.extensions.getByType(DevelocityConfiguration::class.java)
        buildScanDevelocityReporting(target, develocity, gbosEnabled, gbosDevelocityEnabled)
    }

    private fun buildScanDevelocityReporting(
        project: Project,
        buildScanExtension: DevelocityConfiguration,
        gbosEnabled: Provider<Boolean>,
        gbosDevelocityEnabled: Provider<Boolean>,
    ) {
        val providers = project.kotlinProcessProviders()
        val gbosJsonFile = project.layout.buildDirectory.file("info-kotlin-process/gbos.json")
        val gbosNdjsonFile = project.layout.buildDirectory.file("info-kotlin-process/gbos.ndjson")

        buildScanExtension.buildScan.buildFinished {
            val processes = collectKotlinProcesses(providers)
            val develocityEnabled = gbosDevelocityEnabled.get()
            DevelocityValues(
                buildScanExtension,
                processes,
                develocityEnabled,
                legacyEnabled = !develocityEnabled,
            ).addProcessesInfoToBuildScan()
            GbosOutputReport(
                gbosJsonFile.get().asFile,
                gbosNdjsonFile.get().asFile,
            ).write(processes, gbosEnabled.get())
        }
    }
}
