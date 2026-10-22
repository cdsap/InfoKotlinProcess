package io.github.cdsap.kotlinprocess.output

import io.github.cdsap.jdk.tools.parser.model.Process

class ConsoleOutput(
    private val processes: List<Process>,
) {
    fun render(): String = ConsoleTableRenderer(processes).render()
}
