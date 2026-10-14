package io.github.cdsap.kotlinprocess.output

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import io.github.cdsap.jdk.tools.parser.model.Process

class DevelocityValues(
    private val develocityConfiguration: DevelocityConfiguration,
    private val processes: List<Process>,
    private val gbosEnabled: Boolean,
    private val legacyEnabled: Boolean,
) {
    constructor(
        develocityConfiguration: DevelocityConfiguration,
        processes: List<Process>,
        gbosEnabled: Boolean,
    ) : this(develocityConfiguration, processes, gbosEnabled, legacyEnabled = true)

    constructor(
        develocityConfiguration: DevelocityConfiguration,
        processes: List<Process>,
    ) : this(develocityConfiguration, processes, gbosEnabled = false, legacyEnabled = true)

    fun addProcessesInfoToBuildScan() {
        val customValues = DevelocityCustomValues.from(processes, gbosEnabled, legacyEnabled)
        if (customValues.isNotEmpty()) {
            develocityConfiguration.buildScan {
                customValues.forEach { (key, customValue) ->
                    value(key, customValue)
                }
            }
        }
    }
}
