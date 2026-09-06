package io.github.cdsap.kotlinprocess

import junit.framework.TestCase.assertTrue
import org.junit.Test

class KotlinProcessConsolidatorTest {
    private val consolidator = KotlinProcessConsolidator()

    @Test
    fun consolidateReturnsEmptyListForEmptyRawStrings() {
        val processes = consolidator.consolidate("", "")

        assertTrue(processes.isEmpty())
    }

    @Test
    fun consolidateReturnsEmptyListForBlankRawStrings() {
        val processes = consolidator.consolidate("   ", "\n")

        assertTrue(processes.isEmpty())
    }
}
