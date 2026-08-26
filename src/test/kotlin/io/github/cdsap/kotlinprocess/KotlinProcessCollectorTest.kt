package io.github.cdsap.kotlinprocess

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class KotlinProcessCollectorTest {
    @Test
    fun collectReturnsEmptyListForEmptyProviderValues() {
        val project = ProjectBuilder.builder().build()
        val jStat = project.providers.provider { "" }
        val jInfo = project.providers.provider { "" }

        val processes = KotlinProcessCollector().collect(jStat, jInfo)

        assertTrue(processes.isEmpty())
    }

    @Test
    fun collectQueriesProvidersLazily() {
        val project = ProjectBuilder.builder().build()
        var jStatGets = 0
        var jInfoGets = 0
        val jStat =
            project.providers.provider {
                jStatGets++
                ""
            }
        val jInfo =
            project.providers.provider {
                jInfoGets++
                ""
            }

        val collector = KotlinProcessCollector()
        assertEquals(0, jStatGets)
        assertEquals(0, jInfoGets)

        collector.collect(jStat, jInfo)

        assertEquals(1, jStatGets)
        assertEquals(1, jInfoGets)
    }
}
