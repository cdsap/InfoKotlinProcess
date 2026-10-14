package io.github.cdsap.kotlinprocess

import org.gradle.api.provider.ProviderFactory

internal object GbosOptIn {
    const val PROPERTY_JSON = "infoKotlinProcess.gbos.json.enabled"
    const val PROPERTY_NDJSON = "infoKotlinProcess.gbos.ndjson.enabled"
    const val PROPERTY_DEVELOCITY = "infoKotlinProcess.gbos.develocity.enabled"
    const val PROPERTY_LEGACY = "infoKotlinProcess.legacy.enabled"

    fun configureConventions(
        extension: GbosReportingExtension,
        providers: ProviderFactory,
    ) {
        extension.json.convention(providers.gradleProperty(PROPERTY_JSON).map(String::toBoolean).orElse(false))
        extension.ndjson.convention(providers.gradleProperty(PROPERTY_NDJSON).map(String::toBoolean).orElse(false))
        extension.develocity.convention(
            providers.gradleProperty(PROPERTY_DEVELOCITY).map(String::toBoolean).orElse(false),
        )
    }

    fun configureLegacyConventions(
        extension: LegacyReportingExtension,
        providers: ProviderFactory,
    ) {
        extension.enabled.convention(
            providers.gradleProperty(PROPERTY_LEGACY).map(String::toBoolean).orElse(true),
        )
    }
}
