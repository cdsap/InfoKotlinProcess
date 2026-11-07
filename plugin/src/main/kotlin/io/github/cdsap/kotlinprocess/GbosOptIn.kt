package io.github.cdsap.kotlinprocess

import org.gradle.api.provider.ProviderFactory

internal object GbosOptIn {
    const val PROPERTY = "infoKotlinProcess.gbos.enabled"

    fun configureConventions(
        extension: GbosReportingExtension,
        providers: ProviderFactory,
    ) {
        extension.enabled.convention(providers.gradleProperty(PROPERTY).map(String::toBoolean).orElse(false))
    }
}
