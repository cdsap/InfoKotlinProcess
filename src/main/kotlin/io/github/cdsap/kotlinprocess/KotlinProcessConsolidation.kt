package io.github.cdsap.kotlinprocess

import io.github.cdsap.jdk.tools.parser.ConsolidateProcesses
import io.github.cdsap.jdk.tools.parser.model.Process
import io.github.cdsap.jdk.tools.parser.model.TypeProcess
import io.github.cdsap.valuesourceprocess.jInfo
import io.github.cdsap.valuesourceprocess.jStat
import org.gradle.api.Project
import org.gradle.api.provider.Provider

fun consolidateKotlinProcesses(
    jStatOutput: String,
    jInfoOutput: String,
): List<Process> = ConsolidateProcesses().consolidate(jStatOutput, jInfoOutput, TypeProcess.Kotlin)

data class KotlinProcessProviders(
    val jStat: Provider<String>,
    val jInfo: Provider<String>,
)

fun Project.kotlinProcessProviders(): KotlinProcessProviders =
    KotlinProcessProviders(
        jStat = jStat(Constants.KOTLIN_PROCESS_NAME),
        jInfo = jInfo(Constants.KOTLIN_PROCESS_NAME),
    )
