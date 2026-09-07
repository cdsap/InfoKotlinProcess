package io.github.cdsap.kotlinprocess

import junit.framework.TestCase.assertTrue
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class KotlinProcessCollectorTest {
    private val collector = KotlinProcessCollector()

    @Test
    fun collectReturnsEmptyListForEmptyProviders() {
        val project = ProjectBuilder.builder().build()
        val processes =
            collector.collect(
                project.provider { "" },
                project.provider { "" },
            )

        assertTrue(processes.isEmpty())
    }

    @Test
    fun collectReturnsEmptyListForBlankProviders() {
        val project = ProjectBuilder.builder().build()
        val processes =
            collector.collect(
                project.provider { "   " },
                project.provider { "\n" },
            )

        assertTrue(processes.isEmpty())
    }
}
