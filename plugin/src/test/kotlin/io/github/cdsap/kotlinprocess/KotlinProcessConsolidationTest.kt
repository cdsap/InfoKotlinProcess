package io.github.cdsap.kotlinprocess

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
}
