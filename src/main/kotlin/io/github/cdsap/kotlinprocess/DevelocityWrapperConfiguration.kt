package io.github.cdsap.kotlinprocess

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import io.github.cdsap.kotlinprocess.output.DevelocityValues
import io.github.cdsap.valuesourceprocess.jInfo
import io.github.cdsap.valuesourceprocess.jStat
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
        val jStat = project.jStat(Constants.KOTLIN_PROCESS_NAME)
        val jInfo = project.jInfo(Constants.KOTLIN_PROCESS_NAME)

        buildScanExtension.buildScan.buildFinished {
            val processes = KotlinProcessCollector().collect(jStat.get(), jInfo.get())
            DevelocityValues(buildScanExtension, processes).addProcessesInfoToBuildScan()
        }
    }
}
