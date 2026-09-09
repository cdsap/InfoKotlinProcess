package io.github.cdsap.kotlinprocess

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
}
