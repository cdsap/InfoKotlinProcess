package io.github.cdsap.kotlinprocess

import org.gradle.api.provider.ProviderFactory

internal object GbosOptIn {
    const val PROPERTY = "infoKotlinProcess.gbos.enabled"
    const val DEVELOCITY_PROPERTY = "infoKotlinProcess.gbos.develocity.enabled"

    fun configureConventions(
        extension: GbosReportingExtension,
        providers: ProviderFactory,
    ) {
        extension.enabled.convention(providers.gradleProperty(PROPERTY).map(String::toBoolean).orElse(false))
        extension.develocity.convention(
            providers.gradleProperty(DEVELOCITY_PROPERTY).map(String::toBoolean).orElse(false),
        )
    }
}
