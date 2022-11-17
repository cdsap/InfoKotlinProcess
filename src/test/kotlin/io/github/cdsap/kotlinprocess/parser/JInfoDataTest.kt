package io.github.cdsap.kotlinprocess.parser

import junit.framework.TestCase.assertTrue
import org.junit.Test

class JInfoDataTest {

    @Test
    fun testIncorrectOutputGeneratesEmptyMap() {
        val jInfoData = JInfoData()
        val result = jInfoData.process("cxxxxxd")
        assertTrue(result.isEmpty())
    }

    @Test
    fun testEmptyOutputGeneratesEmptyMap() {
        val jInfoData = JInfoData()
        val result = jInfoData.process("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun testResultIsNotIncludingMaxHeapSize() {
        val jInfoData = JInfoData()
        val result = jInfoData.process("XX:CICompilerCount=4 -XX:CompressedClassSpaceSize=260046848")
        assertTrue(result.isEmpty())
    }

    @Test
    fun testResultContainingMultipleProcessesIsParsed() {
        val jInfoData = JInfoData()
        val output = """
            XX:CICompilerCount=4 -XX:CompressedClassSpaceSize=260046848 -XX:MaxHeapSize=2 -XX:ClassSpaceSize=26
            12345
            XX:CICompilerCount=4 -XX:CompressedClassSpaceSize=260046848" -XX:MaxHeapSize=1 -XX:ClassSpaceSize=26
            54321
            """.trimIndent()
        val result = jInfoData.process(output)
        assertTrue(result.isNotEmpty())
        assertTrue(result.containsKey("12345") && result.containsKey("54321"))
        assertTrue(result["12345"]?.max == 2.0)
        assertTrue(result["54321"]?.max == 1.0)
    }

    @Test
    fun testResultContainingMultipleProcessesWithEndingEmptyLineIsParsed() {
        val jInfoData = JInfoData()
        val output = """
            XX:CICompilerCount=4 -XX:CompressedClassSpaceSize=260046848 -XX:MaxHeapSize=2 -XX:ClassSpaceSize=26
            12345
            XX:CICompilerCount=4 -XX:CompressedClassSpaceSize=260046848" -XX:MaxHeapSize=1 -XX:ClassSpaceSize=26
            54321

            """.trimIndent()
        val result = jInfoData.process(output)
        assertTrue(result.isNotEmpty())
        assertTrue(result.containsKey("12345") && result.containsKey("54321"))
        assertTrue(result["12345"]?.max == 2.0)
        assertTrue(result["54321"]?.max == 1.0)
    }


    @Test
    fun testResultContainingMultipleProcessesIsParsedButOneIsIncorrect() {
        val jInfoData = JInfoData()
        val output = """
            XX:CICompilerCount=4 -XX:CompressedClassSpaceSize=260046848 -XX:MaxHeapSize=2 -XX:ClassSpaceSize=26
            12345
            XX:CICompilerCount=4 -XX:CompressedClassSpaceSize=260046848 -XX:ClassSpaceSize=26
            54321
            """.trimIndent()
        val result = jInfoData.process(output)
        assertTrue(result.size == 1)
        assertTrue(result.containsKey("12345"))
        assertTrue(result["12345"]?.max == 2.0)
    }
}
