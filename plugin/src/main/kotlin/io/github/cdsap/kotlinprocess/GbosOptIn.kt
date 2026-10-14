package io.github.cdsap.kotlinprocess

import org.gradle.api.provider.ProviderFactory

internal object GbosOptIn {
    const val PROPERTY_JSON = "infoKotlinProcess.gbos.json.enabled"
    const val PROPERTY_NDJSON = "infoKotlinProcess.gbos.ndjson.enabled"

    fun configureConventions(
        extension: GbosReportingExtension,
        providers: ProviderFactory,
    ) {
        extension.json.convention(providers.gradleProperty(PROPERTY_JSON).map(String::toBoolean).orElse(false))
        extension.ndjson.convention(providers.gradleProperty(PROPERTY_NDJSON).map(String::toBoolean).orElse(false))
    }
}
