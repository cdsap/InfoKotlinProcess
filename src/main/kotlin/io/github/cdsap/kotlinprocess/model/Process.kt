package io.github.cdsap.kotlinprocess.model


data class Process(
    val pid: String,
    val max: Double,
    val usage: Double,
    val capacity: Double,
    val gcTime: Double,
    val uptime: Double,
    val type: String
)
