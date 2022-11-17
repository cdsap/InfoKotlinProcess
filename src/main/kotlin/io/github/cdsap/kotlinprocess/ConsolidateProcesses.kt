package io.github.cdsap.valuesource.commandline

import io.github.cdsap.valuesource.commandline.model.JInfoData
import io.github.cdsap.valuesource.commandline.model.JstatData
import io.github.cdsap.valuesource.commandline.model.Process

class ConsolidateProcesses {

    fun consolidate(jStatResult: String, jInfoResult: String): List<Process> {
        val processesConsolidated = mutableListOf<Process>()
        val jInfoData = JInfoData().process(jInfoResult)
        val jStatData = JstatData().process(jStatResult)
        if (jInfoData.size != jStatData.size) {
            // different number of processes
        } else {
            jInfoData.forEach {
                if (jStatData.contains(it.key)) {
                    processesConsolidated.add(
                        Process(
                            pid = it.key,
                            jInfo = it.value,
                            jstatData = jStatData[it.key]!!
                        )
                    )
                }
            }
        }
        return processesConsolidated
    }
}
