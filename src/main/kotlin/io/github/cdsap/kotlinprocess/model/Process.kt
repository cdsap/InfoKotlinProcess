package io.github.cdsap.valuesource.commandline.model


data class Process(
    val pid: String,
    val jInfo: ProcessJInfo,
    val jstatData: ProcessJstat
)
