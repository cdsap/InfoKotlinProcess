package io.github.cdsap.kotlinprocess

import junit.framework.TestCase.assertTrue
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class KotlinProcessConsolidationTest {
    @Test
    fun consolidateKotlinProcessesReturnsEmptyListForEmptyRawStrings() {
        val processes = consolidateKotlinProcesses("", "")

        assertTrue(processes.isEmpty())
    }

    @Test
    fun consolidateKotlinProcessesReturnsEmptyListForBlankRawStrings() {
        val processes = consolidateKotlinProcesses("   ", "\n")

        assertTrue(processes.isEmpty())
    }

    @Test
    fun kotlinProcessProvidersReturnsProviders() {
        val project = ProjectBuilder.builder().build()
        val providers = project.kotlinProcessProviders()

        assertTrue(providers.jStat.isPresent)
        assertTrue(providers.jInfo.isPresent)
    }
}
