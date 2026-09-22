package io.github.cdsap.kotlinprocess.output

import groovy.json.JsonOutput
import io.github.cdsap.jdk.tools.parser.model.Process
import kotlin.math.roundToLong

internal object DevelocityCustomValues {
    fun from(
        processes: List<Process>,
        gbosEnabled: Boolean,
    ): List<Pair<String, String>> =
        if (gbosEnabled) {
            GbosDevelocityProjection.values(processes)
        } else {
            processes.flatMap(::legacyValues)
        }

    private fun legacyValues(process: Process): List<Pair<String, String>> =
        listOf(
            "Kotlin-Process-${process.pid}-max" to "${process.max} GB",
            "Kotlin-Process-${process.pid}-usage" to "${process.usage} GB",
            "Kotlin-Process-${process.pid}-capacity" to "${process.capacity} GB",
            "Kotlin-Process-${process.pid}-uptime" to "${process.uptime} minutes",
            "Kotlin-Process-${process.pid}-gcTime" to "${process.gcTime} minutes",
            "Kotlin-Process-${process.pid}-gcType" to process.typeGc,
        )
}

internal object GbosDevelocityProjection {
    const val SCHEMA_VERSION = "1.0.0"
    const val CONTRACT_VERSION = "0.0.4"
    const val PRODUCER_NAME = "info-kotlin-process"
    const val PRODUCER_NAMESPACE = "gbos.v1.producer.info_kotlin_process"
    const val OBSERVATION_VALUE = "$PRODUCER_NAMESPACE.observation"

    fun values(processes: List<Process>): List<Pair<String, String>> {
        if (processes.isEmpty()) return emptyList()

        return buildList {
            add("gbos.schema" to SCHEMA_VERSION)
            add("$PRODUCER_NAMESPACE.name" to PRODUCER_NAME)
            add("$PRODUCER_NAMESPACE.version" to CONTRACT_VERSION)
            processes.forEach { process ->
                add(OBSERVATION_VALUE to observation(process))
            }
        }
    }

    private fun observation(process: Process): String {
        val attributes =
            linkedMapOf<String, Any>(
                "process.pid" to
                    requireNotNull(process.pid.toLongOrNull()) {
                        "Kotlin process PID must be numeric: ${process.pid}"
                    },
                "jvm.process.role" to "kotlin-daemon",
            )
        normalizeGcName(process.typeGc)?.let { attributes["jvm.gc.name"] = it }

        val document =
            linkedMapOf(
                "scope" to "jvm.process",
                "aggregationScope" to "entity",
                "attributes" to attributes,
                "measurements" to
                    listOf(
                        measurement("jvm.process.memory.heap.limit", process.max.toBytes(), "By", "last"),
                        measurement("jvm.process.memory.heap.used", process.usage.toBytes(), "By", "last"),
                        measurement("jvm.process.memory.heap.committed", process.capacity.toBytes(), "By", "last"),
                        measurement("jvm.process.gc.time", process.gcTime * SECONDS_PER_MINUTE, "s", "sum"),
                        measurement("jvm.process.uptime", process.uptime * SECONDS_PER_MINUTE, "s", "last"),
                    ),
            )
        return JsonOutput.toJson(document)
    }

    private fun measurement(
        name: String,
        value: Number,
        unit: String,
        aggregation: String,
    ): Map<String, Any> =
        linkedMapOf(
            "name" to name,
            "value" to value,
            "unit" to unit,
            "aggregation" to aggregation,
        )

    private fun Double.toBytes(): Long = (this * BYTES_PER_GIB).roundToLong()

    private fun normalizeGcName(rawName: String): String? =
        when {
            rawName.contains("ZGC", ignoreCase = true) -> "ZGC"
            rawName.contains("Shenandoah", ignoreCase = true) -> "Shenandoah"
            rawName.contains("G1", ignoreCase = true) -> "G1"
            rawName.contains("Parallel", ignoreCase = true) -> "Parallel"
            rawName.contains("Serial", ignoreCase = true) -> "Serial"
            rawName.isNotBlank() && !rawName.startsWith("-XX:") -> rawName
            else -> null
        }

    private const val BYTES_PER_GIB = 1_073_741_824.0
    private const val SECONDS_PER_MINUTE = 60.0
}
