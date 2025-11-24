package io.github.cdsap.kotlinprocess.output

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import io.github.cdsap.jdk.tools.parser.model.Process

class DevelocityValues(
    private val develocityConfiguration: DevelocityConfiguration,
    private val processes: List<Process>,
) {
    fun addProcessesInfoToBuildScan() {
        processes.map {
            develocityConfiguration.buildScan {
                value("Kotlin-Process-${it.pid}-max", "${it.max} GB")
                value("Kotlin-Process-${it.pid}-usage", "${it.usage} GB")
                value("Kotlin-Process-${it.pid}-capacity", "${it.capacity} GB")
                value("Kotlin-Process-${it.pid}-uptime", "${it.uptime} minutes")
                value("Kotlin-Process-${it.pid}-gcTime", "${it.gcTime} minutes")
                value("Kotlin-Process-${it.pid}-gcType", it.typeGc)
            }
        }
    }
}
