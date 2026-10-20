package io.github.cdsap.kotlinprocess.output

import io.github.cdsap.jdk.tools.parser.model.Process
import io.github.cdsap.jdk.tools.parser.model.TypeProcess
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class ConsoleOutputTest {
    @Test
    fun printWritesRenderedTableToProvidedStream() {
        val output = ByteArrayOutputStream()
        val rendered = ConsoleTableRenderer(emptyList()).render()

        ConsoleOutput(emptyList(), PrintStream(output, true, Charsets.UTF_8.name())).print()

        assertEquals(rendered + System.lineSeparator(), output.toString(Charsets.UTF_8.name()))
    }

    @Test
    fun renderIncludesHeadersAndProcessValues() {
        val rendered =
            ConsoleOutput(
                listOf(
                    Process("9011", 1.0, 0.5, 0.75, 0.25, 2.5, "-XX:+UseG1GC", TypeProcess.Kotlin),
                ),
            ).render()

        listOf("Kotlin processes", "PID", "Max", "Usage", "Capacity", "GC Time", "GC Type", "Uptime")
            .forEach { assertTrue("Missing header: $it", rendered.contains(it)) }
        listOf("9011", "1.0 Gb", "0.5 Gb", "0.75 Gb", "0.25 minutes", "-XX:+UseG1GC", "2.5 minutes")
            .forEach { assertTrue("Missing process value: $it", rendered.contains(it)) }
    }

    @Test
    fun renderIncludesHeadersWhenThereAreNoProcesses() {
        val rendered = ConsoleOutput(emptyList()).render()

        listOf("Kotlin processes", "PID", "Max", "Usage", "Capacity", "GC Time", "GC Type", "Uptime")
            .forEach { assertTrue("Missing header: $it", rendered.contains(it)) }
        assertFalse(rendered.contains("minutes"))
    }
}
