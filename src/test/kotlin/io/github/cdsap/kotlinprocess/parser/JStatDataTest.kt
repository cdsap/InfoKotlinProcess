package io.github.cdsap.kotlinprocess.parser

import junit.framework.TestCase.assertTrue
import org.junit.Test

class JStatDataTest {

    @Test
    fun testIncorrectOutputGeneratesEmptyMap() {
        val jStatData = JStatData()
        val result = jStatData.process("xxxx")
        assertTrue(result.isEmpty())
    }

    @Test
    fun testEmptyOutputGeneratesEmptyMap() {
        val jStatData = JStatData()
        val result = jStatData.process("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun testSimpleOutputFromOneProcess() {
        val jStatData = JStatData()
        val result = jStatData.process(
            """
            Timestamp        S0C    S1C    S0U    S1U      EC       EU        OC         OU       MC     MU    CCSC   CCSU   YGC     YGCT    FGC    FGCT    CGC    CGCT     GCT
                    15166.0  0.0   12288.0  0.0   12288.0 89088.0  65536.0   207872.0   87565.3   95824.0 84374.6 11264.0 8760.1     87    0.315   1      0.163  32      0.185    0.663
            42050
        """.trimIndent()
        )
        assertTrue(result.containsKey("42050"))
        assertTrue(result["42050"]?.uptime == 15166.0)
    }

    @Test
    fun testDifferentNumberOfColumnsIsNotProcessed() {
        val jStatData = JStatData()
        val result = jStatData.process(
            """
            Timestamp  s s     S0C    S1C    S0U    S1U      EC       EU        OC         OU       MC     MU    CCSC   CCSU   YGC     YGCT    FGC    FGCT    CGC    CGCT     GCT
                    15166.0  0.0   12288.0  0.0   12288.0 89088.0  65536.0   207872.0   87565.3   95824.0 84374.6 11264.0 8760.1     87    0.315   1      0.163  32      0.185    0.663
            42050
        """.trimIndent()
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun testIncorrectValuesAreNotProcessed() {
        val jStatData = JStatData()
        val result = jStatData.process(
            """
            Timestamp    S0C    S1C    S0U    S1U      EC       EU        OC         OU       MC     MU    CCSC   CCSU   YGC     YGCT    FGC    FGCT    CGC    CGCT     GCT
                    151SSLO66.0  0.0   12288.0  0.0   12288.0 89088.0  65536.0   207872.0   87565.3   95824.0 84374.6 11264.0 8760.1     87    0.315   1      0.163  32      0.185    0.663
            42050
        """.trimIndent()
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun testMultipleProcessesAreParsed() {
        val jStatData = JStatData()
        val result = jStatData.process(
            """
            Timestamp        S0C    S1C    S0U    S1U      EC       EU        OC         OU       MC     MU    CCSC   CCSU   YGC     YGCT    FGC    FGCT    CGC    CGCT     GCT
                    100.0  1.0   2.0  3.0   4.0 5.0  6.0   7.0   8.3   9.0 10.6 11.0 12.1     13    14.315   15      16.163  17      18.185    19.663
            54321
            Timestamp        S0C    S1C    S0U    S1U      EC       EU        OC         OU       MC     MU    CCSC   CCSU   YGC     YGCT    FGC    FGCT    CGC    CGCT     GCT
                    1000.0  10.0   20.0  30.0   40.0 50.0  60.0   70.0   80.3   90.0 100.6 110.0 120.1     130    140.315   150      160.163  170      180.185    190.663
            12345
        """.trimIndent()
        )
        assertTrue(result.containsKey("12345"))
        assertTrue(result.containsKey("54321"))
        assertTrue(result["54321"]?.uptime == 100.0)
        assertTrue(result["54321"]?.gcTime == 19.663)
        assertTrue(result["54321"]?.capacity == (5.0 + 7.0 + 1.0 + 2.0))
        assertTrue(result["54321"]?.usage == (3.0 + 4.0 + 6.0 + 8.3))
        assertTrue(result["12345"]?.uptime == 1000.0)
        assertTrue(result["12345"]?.gcTime == 190.663)
        assertTrue(result["12345"]?.capacity == (50.0 + 70.0 + 10.0 + 20.0))
        assertTrue(result["12345"]?.usage == (30.0 + 40.0 + 60.0 + 80.3))
        assertTrue(result["12345"]?.uptime == 1000.0)
    }
}
