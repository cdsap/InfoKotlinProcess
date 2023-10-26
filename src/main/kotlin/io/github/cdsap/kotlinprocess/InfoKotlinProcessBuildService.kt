package io.github.cdsap.kotlinprocess

import io.github.cdsap.jdk.tools.parser.ConsolidateProcesses
import io.github.cdsap.jdk.tools.parser.model.TypeProcess
import io.github.cdsap.kotlinprocess.output.ConsoleOutput
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class InfoKotlinProcessBuildService :
    BuildService<InfoKotlinProcessBuildService.Params>, AutoCloseable {
    interface Params : BuildServiceParameters {
        var jInfoProvider: Provider<String>
        var jStatProvider: Provider<String>
    }

    override fun close() {
        val processes =
            ConsolidateProcesses().consolidate(
                parameters.jStatProvider.get(),
                parameters.jInfoProvider.get(),
                TypeProcess.Kotlin
            )
        if (processes.isNotEmpty()) {
            ConsoleOutput(processes).print()
        }
    }
}
