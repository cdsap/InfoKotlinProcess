package io.github.cdsap.kotlinprocess

import io.github.cdsap.jdk.tools.parser.ConsolidateProcesses
import io.github.cdsap.jdk.tools.parser.model.Process
import io.github.cdsap.jdk.tools.parser.model.TypeProcess
import org.gradle.api.provider.Provider

class KotlinProcessCollector {
    fun collect(
        jStatProvider: Provider<String>,
        jInfoProvider: Provider<String>,
    ): List<Process> =
        ConsolidateProcesses().consolidate(
            jStatProvider.get(),
            jInfoProvider.get(),
            TypeProcess.Kotlin,
        )
}
