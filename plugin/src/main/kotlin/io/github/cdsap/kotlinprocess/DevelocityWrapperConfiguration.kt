package io.github.cdsap.kotlinprocess

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import io.github.cdsap.kotlinprocess.output.DevelocityValues
import org.gradle.api.Project
import org.gradle.api.provider.Provider

class DevelocityWrapperConfiguration {
    /** @return true when Build Scan reporting was registered from the Develocity extension. */
    fun configureProjectWithDevelocityIfPresent(target: Project): Boolean =
        configureProjectWithDevelocityIfPresent(target, target.providers.provider { false })

    fun configureProjectWithDevelocityIfPresent(
        target: Project,
        gbosEnabled: Provider<Boolean>,
    ): Boolean {
        val develocity = target.extensions.findByType(DevelocityConfiguration::class.java) ?: return false
        buildScanDevelocityReporting(target, develocity, gbosEnabled)
        return true
    }

    fun configureProjectWithDevelocity(target: Project) {
        configureProjectWithDevelocity(target, target.providers.provider { false })
    }

    fun configureProjectWithDevelocity(
        target: Project,
        gbosEnabled: Provider<Boolean>,
    ) {
        val develocity = target.extensions.getByType(DevelocityConfiguration::class.java)
        buildScanDevelocityReporting(target, develocity, gbosEnabled)
    }

    private fun buildScanDevelocityReporting(
        project: Project,
        buildScanExtension: DevelocityConfiguration,
        gbosEnabled: Provider<Boolean>,
    ) {
        val providers = project.kotlinProcessProviders()

        buildScanExtension.buildScan.buildFinished {
            val processes =
                KotlinProcessCollector().collect(
                    providers.jStat.get(),
                    providers.jInfo.get(),
                )
            DevelocityValues(buildScanExtension, processes, gbosEnabled.get())
                .addProcessesInfoToBuildScan()
        }
    }
}
