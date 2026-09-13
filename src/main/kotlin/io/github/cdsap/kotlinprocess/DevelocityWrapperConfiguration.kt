package io.github.cdsap.kotlinprocess

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import io.github.cdsap.kotlinprocess.output.DevelocityValues
import org.gradle.api.Project

class DevelocityWrapperConfiguration {
    /** @return true when Build Scan reporting was registered from the Develocity extension. */
    fun configureProjectWithDevelocityIfPresent(target: Project): Boolean {
        val develocity = target.extensions.findByType(DevelocityConfiguration::class.java) ?: return false
        buildScanDevelocityReporting(target, develocity)
        return true
    }

    fun configureProjectWithDevelocity(target: Project) {
        val develocity = target.extensions.getByType(DevelocityConfiguration::class.java)
        buildScanDevelocityReporting(target, develocity)
    }

    private fun buildScanDevelocityReporting(
        project: Project,
        buildScanExtension: DevelocityConfiguration,
    ) {
        val providers = project.kotlinProcessProviders()

        buildScanExtension.buildScan.buildFinished {
            val processes =
                KotlinProcessCollector().collect(
                    providers.jStat.get(),
                    providers.jInfo.get(),
                )
            DevelocityValues(buildScanExtension, processes).addProcessesInfoToBuildScan()
        }
    }
}
