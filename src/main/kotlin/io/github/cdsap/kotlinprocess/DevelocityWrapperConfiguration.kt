package io.github.cdsap.kotlinprocess

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import io.github.cdsap.kotlinprocess.output.DevelocityValues
import org.gradle.api.Project

class DevelocityWrapperConfiguration {
    fun configureProjectWithDevelocity(target: Project) {
        val extension = target.extensions.findByType(DevelocityConfiguration::class.java) != null
        if (extension) {
            buildScanDevelocityReporting(target, target.extensions.findByType(DevelocityConfiguration::class.java)!!)
        }
    }

    private fun buildScanDevelocityReporting(
        project: Project,
        buildScanExtension: DevelocityConfiguration,
    ) {
        val providers = project.kotlinProcessProviders()

        buildScanExtension.buildScan.buildFinished {
            val processes = consolidateKotlinProcesses(providers.jStat.get(), providers.jInfo.get())
            DevelocityValues(buildScanExtension, processes).addProcessesInfoToBuildScan()
        }
    }
}
