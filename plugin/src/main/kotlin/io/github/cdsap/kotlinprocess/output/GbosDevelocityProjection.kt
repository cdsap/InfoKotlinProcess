package io.github.cdsap.kotlinprocess.output

import io.github.cdsap.jdk.tools.parser.model.Process

internal object DevelocityCustomValues {
    fun from(
        processes: List<Process>,
        gbosEnabled: Boolean,
        legacyEnabled: Boolean = !gbosEnabled,
    ): List<Pair<String, String>> =
        (if (legacyEnabled) processes.flatMap(::legacyValues) else emptyList()) +
            if (gbosEnabled) GbosDevelocityProjection.values(processes) else emptyList()

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
    const val SCHEMA_VERSION = GbosObservations.SCHEMA_VERSION
    const val CONTRACT_VERSION = GbosObservations.CONTRACT_VERSION
    const val PRODUCER_NAME = GbosObservations.PRODUCER_NAME
    const val PRODUCER_NAMESPACE = GbosObservations.PRODUCER_NAMESPACE
    const val OBSERVATION_VALUE = "$PRODUCER_NAMESPACE.observation"

    fun values(processes: List<Process>): List<Pair<String, String>> {
        if (processes.isEmpty()) return emptyList()

        return buildList {
            add("gbos.schema" to SCHEMA_VERSION)
            add("$PRODUCER_NAMESPACE.name" to PRODUCER_NAME)
            add("$PRODUCER_NAMESPACE.version" to CONTRACT_VERSION)
            processes.forEach { process ->
                add(OBSERVATION_VALUE to GbosObservations.observation(process))
            }
        }
    }
}
