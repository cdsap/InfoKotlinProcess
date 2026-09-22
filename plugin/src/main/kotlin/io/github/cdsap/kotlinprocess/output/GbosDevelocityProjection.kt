package io.github.cdsap.kotlinprocess.output

import groovy.json.JsonOutput
import io.github.cdsap.gbos.core.GbosAttributeValue
import io.github.cdsap.gbos.core.GbosMeasurement
import io.github.cdsap.gbos.core.GbosObservation
import io.github.cdsap.gbos.core.GbosProducer
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
            buildMap<String, GbosAttributeValue> {
                put(
                    "process.pid",
                    GbosAttributeValue.Integer(
                        requireNotNull(process.pid.toLongOrNull()) {
                            "Kotlin process PID must be numeric: ${process.pid}"
                        },
                    ),
                )
                put("jvm.process.role", GbosAttributeValue.Text("kotlin-daemon"))
                normalizeGcName(process.typeGc)?.let { put("jvm.gc.name", GbosAttributeValue.Text(it)) }
            }
        val observation =
            GbosObservation(
                schemaVersion = SCHEMA_VERSION,
                producer = GbosProducer(PRODUCER_NAME, CONTRACT_VERSION),
                scope = "jvm.process",
                aggregationScope = "entity",
                attributes = attributes,
                measurements =
                    listOf(
                        GbosMeasurement("jvm.process.memory.heap.limit", process.max.toBytes().toDouble(), "By", "last"),
                        GbosMeasurement("jvm.process.memory.heap.used", process.usage.toBytes().toDouble(), "By", "last"),
                        GbosMeasurement("jvm.process.memory.heap.committed", process.capacity.toBytes().toDouble(), "By", "last"),
                        GbosMeasurement("jvm.process.gc.time", process.gcTime * SECONDS_PER_MINUTE, "s", "sum"),
                        GbosMeasurement("jvm.process.uptime", process.uptime * SECONDS_PER_MINUTE, "s", "last"),
                    ),
            )
        return JsonOutput.toJson(
            linkedMapOf(
                "scope" to observation.scope,
                "aggregationScope" to observation.aggregationScope,
                "attributes" to
                    observation.attributes.mapValues { (_, value) ->
                        when (value) {
                            is GbosAttributeValue.Text -> value.value
                            is GbosAttributeValue.Integer -> value.value
                        }
                    },
                "measurements" to
                    observation.measurements.map { measurement ->
                        linkedMapOf(
                            "name" to measurement.name,
                            "value" to if (measurement.unit == "By") measurement.value.toLong() else measurement.value,
                            "unit" to measurement.unit,
                            "aggregation" to measurement.aggregation,
                        )
                    },
            ),
        )
    }

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
