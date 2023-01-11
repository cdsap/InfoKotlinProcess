package io.github.cdsap.kotlinprocess

import com.gradle.scan.plugin.BuildScanExtension
import io.github.cdsap.kotlinprocess.output.BuildScanOutput
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider

class InfoKotlinProcessPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.gradle.rootProject {
            val buildScanExtension = extensions.findByType(com.gradle.scan.plugin.BuildScanExtension::class.java)
            if (buildScanExtension != null) {
                buildScanReporting(project, buildScanExtension)
            } else {
                consoleReporting(target)
            }
        }
    }

    private fun consoleReporting(project: Project) {
        project.gradle.sharedServices.registerIfAbsent(
            "kotlinProcessService", InfoKotlinProcessBuildService::class.java
        ) {
            parameters.jInfoProvider = project.jInfo()
            parameters.jStatProvider = project.jStat()
        }.get()
    }

    private fun buildScanReporting(
        project: Project,
        buildScanExtension: BuildScanExtension
    ) {
        val jStat = project.jStat()
        val jInfo = project.jInfo()

        buildScanExtension.buildFinished {
            val processes = ConsolidateProcesses().consolidate(jStat.get(), jInfo.get())
            BuildScanOutput(buildScanExtension, processes).addProcessesInfoToBuildScan()
        }
    }
}

fun Project.jStat(): Provider<String> {
    return execute("jps | grep KotlinCompileDaemon | sed 's/KotlinCompileDaemon//' | while read ln; do  jstat -gc -t \$ln; echo \"\$ln\"; done")
}

fun Project.jInfo(): Provider<String> {
    return execute("jps | grep KotlinCompileDaemon | sed 's/KotlinCompileDaemon//' | while read ln; do  jinfo \$ln  | grep \"XX:MaxHeapSize\"; echo \"\$ln\";  done")
}

fun Project.execute(command: String): Provider<String> {
    return providers.of(CommandLineWithOutputValue::class.java) {
        parameters.commands.set(command)
    }
}
