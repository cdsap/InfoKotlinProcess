package io.github.cdsap.kotlinprocess

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class KotlinProcessConsolidationTest {
    @Test
    fun kotlinProcessProvidersReturnsProviders() {
        val project = ProjectBuilder.builder().build()
        val providers = project.kotlinProcessProviders()

        assertTrue(providers.jStat.isPresent)
        assertTrue(providers.jInfo.isPresent)
    }

    @Test
    fun collectKotlinProcessesMaterializesProvidersAndDelegatesToCollector() {
        val project = ProjectBuilder.builder().build()
        val jStat = project.providers.provider { jStatWithPid }
        val jInfo = project.providers.provider { jInfoWithPid }

        assertEquals(
            KotlinProcessCollector().collect(jStatWithPid, jInfoWithPid),
            collectKotlinProcesses(jStat, jInfo),
        )
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
