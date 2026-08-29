package io.github.cdsap.kotlinprocess

import io.github.cdsap.jdk.tools.parser.ConsolidateProcesses
import io.github.cdsap.jdk.tools.parser.model.Process
import io.github.cdsap.jdk.tools.parser.model.TypeProcess
import org.gradle.api.provider.Provider

class KotlinProcessCollector {
    fun collect(
        jStat: Provider<String>,
        jInfo: Provider<String>,
    ): List<Process> = collect(jStat.get(), jInfo.get())

    fun collect(
        jStat: String,
        jInfo: String,
    ): List<Process> = ConsolidateProcesses().consolidate(jStat, jInfo, TypeProcess.Kotlin)
}
