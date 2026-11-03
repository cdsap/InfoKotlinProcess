package io.github.cdsap.kotlinprocess

import io.github.cdsap.kotlinprocess.output.ConsoleOutput
import io.github.cdsap.kotlinprocess.output.GbosOutputReport
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener

abstract class InfoKotlinProcessBuildService :
    BuildService<InfoKotlinProcessBuildService.Params>,
    AutoCloseable,
    OperationCompletionListener {
    interface Params : BuildServiceParameters {
        var jInfoProvider: Provider<String>
        var jStatProvider: Provider<String>
        val gbosEnabled: Property<Boolean>
        val gbosJsonFile: RegularFileProperty
        val gbosNdjsonFile: RegularFileProperty
    }

    override fun close() {
        val processes =
            collectKotlinProcesses(
                KotlinProcessProviders(
                    jStat = parameters.jStatProvider,
                    jInfo = parameters.jInfoProvider,
                ),
            )
        if (processes.isNotEmpty()) {
            println(ConsoleOutput(processes).render())
        }
        GbosOutputReport(parameters.gbosJsonFile.get().asFile, parameters.gbosNdjsonFile.get().asFile)
            .write(processes, parameters.gbosEnabled.get())
    }

    override fun onFinish(event: FinishEvent?) {}
}
