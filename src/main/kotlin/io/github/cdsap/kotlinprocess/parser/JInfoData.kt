package io.github.cdsap.kotlinprocess.parser

import io.github.cdsap.kotlinprocess.model.ProcessJInfo


class JInfoData {

    fun process(result: String): Map<String, ProcessJInfo> {
        val processP = mutableMapOf<String, ProcessJInfo>()
        val lines = result.split("\n")
        if (lines.last().trim() == "") {
            lines.dropLast(1)
        }
        val xNumberOfProcess = lines.size / 2
        var auxIndex = 0
        for (i in 0 until xNumberOfProcess) {
            val flags = lines[auxIndex].split("\\s+".toRegex())
            if (!flags.last().contains("-XX:MaxHeapSize=")) {
                flags.dropLast(1)
            }
            val heapSizeFlag = flags.firstOrNull { it.contains("-XX:MaxHeapSize=") }?.replace("-XX:MaxHeapSize=", "")
            val process = lines[++auxIndex].split("\\s+".toRegex())
            if (heapSizeFlag != null) {
                auxIndex++
                processP[process.first()] = ProcessJInfo(heapSizeFlag.toDouble())
            }
        }
        return processP
    }

}
