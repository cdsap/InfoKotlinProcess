package io.github.cdsap.kotlinprocess

import io.github.cdsap.jdk.tools.parser.ConsolidateProcesses
import io.github.cdsap.jdk.tools.parser.model.Process
import io.github.cdsap.jdk.tools.parser.model.TypeProcess

class KotlinProcessConsolidator {
    fun consolidate(
        jStatOutput: String,
        jInfoOutput: String,
    ): List<Process> = ConsolidateProcesses().consolidate(jStatOutput, jInfoOutput, TypeProcess.Kotlin)
}
