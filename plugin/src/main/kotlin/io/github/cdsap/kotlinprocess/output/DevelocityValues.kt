package io.github.cdsap.kotlinprocess.output

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import io.github.cdsap.jdk.tools.parser.model.Process

class DevelocityValues(
    private val develocityConfiguration: DevelocityConfiguration,
    private val processes: List<Process>,
    private val gbosEnabled: Boolean,
) {
    constructor(
        develocityConfiguration: DevelocityConfiguration,
        processes: List<Process>,
    ) : this(develocityConfiguration, processes, gbosEnabled = false)

    fun addProcessesInfoToBuildScan() {
        val customValues = DevelocityCustomValues.from(processes, gbosEnabled)
        if (customValues.isNotEmpty()) {
            develocityConfiguration.buildScan {
                customValues.forEach { (key, customValue) ->
                    value(key, customValue)
                }
            }
        }
    }
}
