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
            val gcType = getCollector(flags)
            if (!flags.last().contains("-XX:MaxHeapSize=")) {
                flags.dropLast(1)
            }

            val heapSizeFlag = flags.firstOrNull { it.contains("-XX:MaxHeapSize=") }?.replace("-XX:MaxHeapSize=", "")
            val process = lines[++auxIndex].split("\\s+".toRegex())
            if (heapSizeFlag != null) {
                auxIndex++
                processP[process.first()] = ProcessJInfo(heapSizeFlag.toDouble(), gcType)
            }
        }
        return processP
    }

    private fun getCollector(flags: List<String>): String {
        if (flags.contains("-XX:+UseZGC")) {
            return "-XX:+UseZGC"
        } else if (flags.contains("-XX:+UseSerialGC")) {
            return "-XX:+UseSerialGC"
        } else if (flags.contains("-XX:+UseShenandoahGC")) {
            return "-XX:+UseShenandoahGC"
        } else if (flags.contains("-XX:+UseG1GC")) {
            return "-XX:+UseG1GC"
        } else if (flags.contains("-XX:+UseParallelGC")) {
            return "-XX:+UseParallelGC"
        } else {
            return ""
        }
    }
}
