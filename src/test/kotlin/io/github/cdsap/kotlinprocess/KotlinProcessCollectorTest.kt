package io.github.cdsap.kotlinprocess

import junit.framework.TestCase.assertTrue
import org.junit.Test

class KotlinProcessCollectorTest {
    @Test
    fun collectReturnsEmptyListForEmptyRawStrings() {
        val processes = KotlinProcessCollector().collect("", "")

        assertTrue(processes.isEmpty())
    }

    @Test
    fun collectReturnsEmptyListForBlankRawStrings() {
        val processes = KotlinProcessCollector().collect("   ", "\n")

        assertTrue(processes.isEmpty())
    }
}
