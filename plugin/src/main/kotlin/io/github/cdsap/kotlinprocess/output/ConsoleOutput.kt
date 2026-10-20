package io.github.cdsap.kotlinprocess.output

import io.github.cdsap.jdk.tools.parser.model.Process
import java.io.PrintStream

class ConsoleOutput(
    private val processes: List<Process>,
    private val output: PrintStream = System.out,
) {
    fun render(): String = ConsoleTableRenderer(processes).render()

    fun print() {
        output.println(render())
    }
}
