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
            val rawHeaders = lines[currentIndex].split("\\s+".toRegex()).filter { it != "" }
            val rawValues = lines[++currentIndex].split("\\s+".toRegex()).filter { it != "" }
            val mapOfValues = getMapValues(rawHeaders, rawValues)
            val process = lines[++currentIndex].split("\\s+".toRegex())
            currentIndex++
            processes[process.first()] = ProcessJstat(
                capacity = getCapacity(mapOfValues),
                usage = getUsage(mapOfValues),
                gcTime = gcTime(mapOfValues),
                uptime = uptime(mapOfValues)
            )
        }
        return processes
    }

    private fun getMapValues(rawHeaders: List<String>, rawValues: List<String>): Map<String, String> {
        val parsedValues = mutableMapOf<String, String>()
        var i = 0
        rawHeaders.forEach {
            parsedValues[it] = rawValues[i]
            i++
        }
        return parsedValues
    }
}

private fun getCapacity(values: Map<String, String>): Double {
    // ZGC is not Current survivor space
    if (values["S0C"] == "-") {
        val oc = values["OC"]
        val mc = values["MC"]
        if (oc != null && mc != null) {
            val ocNumber = oc.jstatValueToDouble()
            val mcNumber = mc.jstatValueToDouble()
            return mcNumber + ocNumber
        } else {
            return 0.0
        }
    } else {
        val ec = values["EC"]
        val oc = values["OC"]
        val soc = values["S0C"]
        val s1c = values["S1C"]
        if (ec != null && oc != null && soc != null && s1c != null) {
            val ecNumber = ec.jstatValueToDouble()
            val ocNumber = oc.jstatValueToDouble()
            val socNumber = soc.jstatValueToDouble()
            val s1cNumber = s1c.jstatValueToDouble()
            return ecNumber + ocNumber + socNumber + s1cNumber

        } else {
            return 0.0
        }
    }
}

private fun getUsage(values: Map<String, String>): Double {
    // ZGC is not using Eden region
    if (values["S0C"] == "-") {
        val ou = values["OU"]
        val mu = values["MU"]
        if (ou != null && mu != null) {
            val ouNumber = ou.jstatValueToDouble()
            val muNumber = mu.jstatValueToDouble()
            return muNumber + ouNumber
        } else {
            return 0.0
        }
    } else {
        val eu = values["EU"]
        val ou = values["OU"]
        val sou = values["S0U"]
        val s1u = values["S1U"]
        if (eu != null && ou != null && sou != null && s1u != null) {
            val euNumber = eu.jstatValueToDouble()
            val ouNumber = ou.jstatValueToDouble()
            val souNumber = sou.jstatValueToDouble()
            val s1uNumber = s1u.jstatValueToDouble()
            return euNumber + ouNumber + souNumber + s1uNumber

        } else {
            return 0.0
        }

    }
}

private fun gcTime(values: Map<String, String>): Double {
    val gct = values["GCT"]
    return gct?.jstatValueToDouble() ?: 0.0
}

private fun uptime(values: Map<String, String>): Double {
    val timeStamp = values["Timestamp"]
    return timeStamp?.jstatValueToDouble() ?: 0.0
}

fun String.jstatValueToDouble(): Double {
    return try {
        this.toDouble()
    } catch (e: java.lang.NumberFormatException) {
        0.0
    }
}
