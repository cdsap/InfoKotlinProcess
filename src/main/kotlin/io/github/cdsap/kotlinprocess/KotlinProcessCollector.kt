package io.github.cdsap.kotlinprocess

import io.github.cdsap.jdk.tools.parser.ConsolidateProcesses
import io.github.cdsap.jdk.tools.parser.model.Process
import io.github.cdsap.jdk.tools.parser.model.TypeProcess
import io.github.cdsap.valuesourceprocess.jInfo
import io.github.cdsap.valuesourceprocess.jStat
import org.gradle.api.Project
import org.gradle.api.provider.Provider

class KotlinProcessCollector {
    fun providers(project: Project): Pair<Provider<String>, Provider<String>> {
        val jStat = project.jStat(Constants.KOTLIN_PROCESS_NAME)
        val jInfo = project.jInfo(Constants.KOTLIN_PROCESS_NAME)
        return Pair(jStat, jInfo)
    }

    fun collect(
        jStat: Provider<String>,
        jInfo: Provider<String>,
    ): List<Process> = collect(jStat.get(), jInfo.get())

    fun collect(
        jStat: String,
        jInfo: String,
    ): List<Process> = ConsolidateProcesses().consolidate(jStat, jInfo, TypeProcess.Kotlin)
}
