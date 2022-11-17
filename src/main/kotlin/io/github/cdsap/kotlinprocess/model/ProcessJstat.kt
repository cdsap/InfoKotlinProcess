package io.github.cdsap.valuesource.commandline.model

data class ProcessJstat(
    val usage: Double,
    val capacity: Double,
    val gcTime: Double,
    val uptime: Double
)

