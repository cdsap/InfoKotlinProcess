package io.github.cdsap.kotlinprocess.output

import com.gradle.scan.plugin.BuildScanExtension
import io.github.cdsap.kotlinprocess.model.Process

class BuildScanOutput(
    private val buildScanExtension: BuildScanExtension,
    private val processes: List<Process>,
) {

    fun addProcessesInfoToBuildScan() {
        processes.map {
            buildScanExtension.value(
                "Kotlin-Process-${it.pid}-max",
                "${it.max} GB"
            )
            buildScanExtension.value(
                "Kotlin-Process-${it.pid}-usage",
                "${it.usage} GB"
            )
            buildScanExtension.value(
                "Kotlin-Process-${it.pid}-capacity",
                "${it.capacity} GB"
            )
            buildScanExtension.value(
                "Kotlin-Process-${it.pid}-uptime",
                "${it.uptime} minutes"
            )
            buildScanExtension.value(
                "Kotlin-Process-${it.pid}-gcTime",
                "${it.gcTime} minutes"
            )
            buildScanExtension.value(
                "Kotlin-Process-${it.pid}-gcType",
                it.type
            )
        }
    }
}
