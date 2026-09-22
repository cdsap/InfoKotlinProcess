package io.github.cdsap.kotlinprocess

import org.gradle.api.provider.ProviderFactory

internal object GbosOptIn {
    const val PROPERTY_DEVELOCITY = "infoKotlinProcess.gbos.develocity.enabled"

    fun configureConventions(
        extension: GbosReportingExtension,
        providers: ProviderFactory,
    ) {
        extension.develocity.convention(
            providers.gradleProperty(PROPERTY_DEVELOCITY).map(String::toBoolean).orElse(false),
        )
    }
}
