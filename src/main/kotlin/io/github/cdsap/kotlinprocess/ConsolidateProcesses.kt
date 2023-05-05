package io.github.cdsap.kotlinprocess

import io.github.cdsap.kotlinprocess.parser.JInfoData
import io.github.cdsap.kotlinprocess.parser.JStatData
import io.github.cdsap.kotlinprocess.model.Process
import kotlin.math.pow
import kotlin.math.roundToInt

class ConsolidateProcesses {

    fun consolidate(jStatResult: String, jInfoResult: String): List<Process> {
        val processesConsolidated = mutableListOf<Process>()
        val jInfoData = JInfoData().process(jInfoResult)
        val jStatData = JStatData().process(jStatResult)
        if (jInfoData.size != jStatData.size) {
            // different number of processes
            // ignore consolidation
        } else {
            jInfoData.forEach {
                if (jStatData.contains(it.key)) {
                    processesConsolidated.add(
                        Process(
                            pid = it.key,
                            max = it.value.max.toGigsFromBytes(),
                            usage = jStatData[it.key]?.usage?.toGigsFromKb()!!,
                            capacity = jStatData[it.key]?.capacity?.toGigsFromKb()!!,
                            gcTime = jStatData[it.key]?.gcTime?.toMinutes()!!,
                            uptime = jStatData[it.key]?.uptime?.toMinutes()!!,
                            type = jStatData[it.key]?.typeGC!!
                        )
                    )
                }
            }
        }
        return processesConsolidated
    }
}

fun Double.roundTo(numFractionDigits: Int): Double {
    val factor = 10.0.pow(numFractionDigits.toDouble())
    return (this * factor).roundToInt() / factor
}

fun Double.toGigsFromBytes(): Double {
    return (this / (1048576 * 1024)).roundTo(2)
}

fun Double.toGigsFromKb(): Double {
    return (this / 1048576).roundTo(2)
}

fun Double.toMinutes(): Double {
    return (this / 60).roundTo(2)
}
