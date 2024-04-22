package io.github.cdsap.kotlinprocess

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import com.gradle.scan.plugin.BuildScanExtension
import io.github.cdsap.jdk.tools.parser.ConsolidateProcesses
import io.github.cdsap.jdk.tools.parser.model.Process
import io.github.cdsap.jdk.tools.parser.model.TypeProcess
import io.github.cdsap.kotlinprocess.output.DevelocityValues
import io.github.cdsap.kotlinprocess.output.EnterpriseValues
import io.github.cdsap.valuesourceprocess.jInfo
import io.github.cdsap.valuesourceprocess.jStat
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider


class InfoKotlinProcessPlugin : Plugin<Project> {
    private val nameProcess = "KotlinCompileDaemon"
    override fun apply(target: Project) {
        target.gradle.rootProject {

            val develocityConfiguration = extensions.findByType(DevelocityConfiguration::class.java)
            val enterpriseExtension = extensions.findByType(com.gradle.scan.plugin.BuildScanExtension::class.java)

            if (develocityConfiguration != null) {
                buildScanDevelocityReporting(project, develocityConfiguration)
            } else if (enterpriseExtension != null) {
                buildScanEnterpriseReporting(project, enterpriseExtension)
            } else {
                consoleReporting(target)
            }
        }
    }

    private fun consoleReporting(project: Project) {
        project.gradle.sharedServices.registerIfAbsent(
            "kotlinProcessService", InfoKotlinProcessBuildService::class.java
        ) {
            parameters.jInfoProvider = project.jInfo(nameProcess)
            parameters.jStatProvider = project.jStat(nameProcess)
        }.get()
    }

    private fun buildScanEnterpriseReporting(
        project: Project,
        buildScanExtension: BuildScanExtension
    ) {
        val (jStat, jInfo) = providerPair(project)

        buildScanExtension.buildFinished {
            val processes = processes(jStat, jInfo)
            EnterpriseValues(buildScanExtension, processes).addProcessesInfoToBuildScan()
        }
    }

    private fun buildScanDevelocityReporting(
        project: Project,
        buildScanExtension: DevelocityConfiguration
    ) {
        val (jStat, jInfo) = providerPair(project)

        buildScanExtension.buildScan.buildFinished {
            val processes = processes(jStat, jInfo)
            DevelocityValues(buildScanExtension, processes).addProcessesInfoToBuildScan()
        }
    }

    private fun processes(
        jStat: Provider<String>,
        jInfo: Provider<String>
    ): List<Process> {
        val processes = ConsolidateProcesses().consolidate(jStat.get(), jInfo.get(), TypeProcess.Kotlin)
        return processes
    }

    private fun providerPair(project: Project): Pair<Provider<String>, Provider<String>> {
        val jStat = project.jStat(nameProcess)
        val jInfo = project.jInfo(nameProcess)
        return Pair(jStat, jInfo)
    }
}
