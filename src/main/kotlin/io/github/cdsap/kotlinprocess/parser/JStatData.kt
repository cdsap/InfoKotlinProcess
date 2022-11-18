package io.github.cdsap.kotlinprocess.parser

import io.github.cdsap.kotlinprocess.model.ProcessJstat

class JStatData {

    fun process(result: String): Map<String, ProcessJstat> {
        // More than one Kotlin compiler may exist
        // the format out the output is 3 lines per process:
        // Header: Timestamp    S0C    S1C    S0U    S1U      EC       EU        OC         OU       MC     MU    CCSC   CCSU   YGC     YGCT    FGC    FGCT    CGC    CGCT     GCT
        // Values:     1117.8  0.0   30720.0  0.0   30720.0 1135616.0 755712.0  865280.0   546816.0  195184.0 189433.3 22208.0 20357.8     22    0.682   0      0.000  12      0.070    0.752
        // PID 28743
        val processes = mutableMapOf<String, ProcessJstat>()
        val lines = result.split("\n")
        if (lines.last().trim() == "") {
            lines.dropLast(1)
        }
        val numberOfProcessesDetected = lines.size / 3
        var currentIndex = 0

        for (i in 0 until numberOfProcessesDetected) {
            val headers = lines[currentIndex].split("\\s+".toRegex()).filter { it != "" }
            val value = lines[++currentIndex].split("\\s+".toRegex()).filter { it != "" }
            if (headers.size == value.size && checkValuesAraValid(value)) {
                val process = lines[++currentIndex].split("\\s+".toRegex())
                val jspMapValues = mutableMapOf<String, Double>()
                var aux = 0
                currentIndex++

                headers.forEach {
                    jspMapValues[it] = value[aux].toDouble()
                    aux++
                }
                processes[process.first()] = ProcessJstat(
                    capacity = totalCapacity(jspMapValues),
                    usage = usage(jspMapValues),
                    gcTime = gcTime(jspMapValues),
                    uptime = uptime(jspMapValues)
                )

            }
        }

        return processes
    }

    private fun checkValuesAraValid(jspMapValues: List<String>): Boolean {
        jspMapValues.forEach {
            try {
                it.toDouble()
            } catch (e: java.lang.NumberFormatException) {
                return false
            }
        }
        return true
    }

    private fun totalCapacity(jspMapValues: Map<String, Double>): Double {
        return jspMapValues["EC"]!! + jspMapValues["OC"]!! + jspMapValues["S0C"]!! + jspMapValues["S1C"]!!
    }

    private fun usage(jspMapValues: Map<String, Double>): Double {
        return jspMapValues["S0U"]!! + jspMapValues["S1U"]!! + jspMapValues["EU"]!! + jspMapValues["OU"]!!
    }

    private fun gcTime(jspMapValues: Map<String, Double>): Double {
        return jspMapValues["GCT"]!!
    }

    private fun uptime(jspMapValues: Map<String, Double>): Double {
        return jspMapValues["Timestamp"]!!
    }
}
