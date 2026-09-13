package io.github.cdsap.kotlinprocess

import io.github.cdsap.jdk.tools.parser.model.TypeProcess
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

class KotlinProcessCollectorTest {
    private val collector = KotlinProcessCollector()

    @Test
    fun collectReturnsEmptyListForEmptyRawStrings() {
        val processes = collector.collect("", "")

        assertTrue(processes.isEmpty())
    }

    @Test
    fun collectReturnsEmptyListForBlankRawStrings() {
        val processes = collector.collect("   ", "\n")

        assertTrue(processes.isEmpty())
    }

    @Test
    fun collectReturnsEmptyListWhenRawProcessInfoHasNoMatchingProcesses() {
        val processes = collector.collect("xxxx", "yyyy")

        assertTrue(processes.isEmpty())
    }

    @Test
    fun collectConsolidatesRawProcessInfoAsKotlinTypeProcess() {
        val processes = collector.collect(jStatWithPid, jInfoWithPid)

        assertEquals(1, processes.size)
        assertEquals("28743", processes[0].pid)
        assertEquals(TypeProcess.Kotlin, processes[0].typeProcess)
        assertEquals("-XX:+UseParallelGC", processes[0].typeGc)
    }

    private val jStatWithPid =
        """
        Timestamp         S0C         S1C         S0U         S1U          EC          EU          OC          OU          MC          MU        CCSC       CCSU     YGC     YGCT     FGC    FGCT     CGC    CGCT       GCT
                1117.8          0.0      30720.0          0.0      30720.0    1135616.0     755712.0     865280.0     546816.0     195184.0    189433.3      22208.0    20357.8     22    0.682     0    0.000      12    0.070     0.752
        28743
        """.trimIndent()

    private val jInfoWithPid =
        """
        -XX:+UseParallelGC -XX:MaxHeapSize=536870912
        28743
        """.trimIndent()
}
