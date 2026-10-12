package io.github.cdsap.kotlinprocess

import io.github.cdsap.jdk.tools.parser.model.Process
import io.github.cdsap.valuesourceprocess.jInfo
import io.github.cdsap.valuesourceprocess.jStat
import org.gradle.api.Project
import org.gradle.api.provider.Provider

data class KotlinProcessProviders(
    val jStat: Provider<String>,
    val jInfo: Provider<String>,
)

fun Project.kotlinProcessProviders(): KotlinProcessProviders =
    KotlinProcessProviders(
        jStat = jStat(Constants.KOTLIN_PROCESS_NAME),
        jInfo = jInfo(Constants.KOTLIN_PROCESS_NAME),
    )

internal fun collectKotlinProcesses(providers: KotlinProcessProviders): List<Process> =
    KotlinProcessCollector().collect(
        providers.jStat.get(),
        providers.jInfo.get(),
    )
