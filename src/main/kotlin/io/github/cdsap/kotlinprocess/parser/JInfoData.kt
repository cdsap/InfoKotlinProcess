package io.github.cdsap.kotlinprocess.model


class JInfoData {

    fun process(result: String): Map<String, ProcessJInfo> {
        val processP = mutableMapOf<String, ProcessJInfo>()
        val linesJinfo = result.split("\n").dropLast(1)
        val xNumberOfProcess = linesJinfo.size / 2
        var auxIndex = 0
        for (i in 0 until xNumberOfProcess) {
            val vmFlags = linesJinfo[auxIndex].split("\\s+".toRegex()).dropLast(1)
                .first { it.contains("-XX:MaxHeapSize=") }.replace("-XX:MaxHeapSize=", "")
            val process = linesJinfo[++auxIndex].split("\\s+".toRegex())
            auxIndex++
            processP[process.first()] = ProcessJInfo(vmFlags.toDouble())
        }
        return processP
    }

}
