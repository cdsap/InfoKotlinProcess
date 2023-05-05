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

            val typeOfCollector = getCollector(rawHeaders, rawValues)

            val (headers, value) = preparePairsByCollector(typeOfCollector, rawHeaders, rawValues)

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
                    capacity = totalCapacity(typeOfCollector, jspMapValues),
                    usage = usage(typeOfCollector, jspMapValues),
                    gcTime = gcTime(jspMapValues),
                    uptime = uptime(jspMapValues),
                    typeGC = typeOfCollector.name
                )

            }
        }
        return processes
    }

    private fun getCollector(rawHeaders: List<String>, rawValues: List<String>): TypeCollector {
        val socHeaderPosition = rawHeaders.indexOf("S0C")
        val soc = rawValues[socHeaderPosition]
        if (soc == "-") {
            return TypeCollector.Z
        } else {
            val socCGC = rawHeaders.indexOf("CGC")
            val cgc = rawValues[socCGC]
            if (cgc == "-") {
                return TypeCollector.PARALLEL
            } else {
                return TypeCollector.G1
            }
        }
    }

    private fun preparePairsByCollector(
        typeOfCollector: TypeCollector,
        rawHeaders: List<String>,
        rawValues: List<String>
    ): Pair<List<String>, List<String>> {
        when (typeOfCollector) {
            TypeCollector.G1 -> {
                return Pair(rawHeaders, rawValues)
            }

            TypeCollector.PARALLEL -> {
                val concurrentGCTime = rawHeaders.indexOf("CGC")
                val concurrentGCTimeTotal = rawHeaders.indexOf("CGCT")

                val headers = rawHeaders.toMutableList()
                headers.removeAt(concurrentGCTime)
                headers.removeAt(concurrentGCTimeTotal - 1)
                val value = rawValues.toMutableList()
                value.removeAt(concurrentGCTime)
                value.removeAt(concurrentGCTimeTotal - 1)
                return Pair(headers.toList(), value.toList())
            }

            TypeCollector.Z -> {
                val soc = rawHeaders.indexOf("S0C")
                val s1c = rawHeaders.indexOf("S1C")
                val sou = rawHeaders.indexOf("S0U")
                val s1u = rawHeaders.indexOf("S1U")
                val ec = rawHeaders.indexOf("EC")
                val eu = rawHeaders.indexOf("EU")
                val ygc = rawHeaders.indexOf("YGC")
                val ygct = rawHeaders.indexOf("YGCT")
                val fgc = rawHeaders.indexOf("FGC")
                val fgct = rawHeaders.indexOf("FGCT")

                val headers = rawHeaders.toMutableList()
                headers.removeAt(soc)
                headers.removeAt(s1c - 1)
                headers.removeAt(sou - 2)
                headers.removeAt(s1u - 3)
                headers.removeAt(ec - 4)
                headers.removeAt(eu - 5)
                headers.removeAt(ygc - 6)
                headers.removeAt(ygct - 7)
                headers.removeAt(fgc - 8)
                headers.removeAt(fgct - 9)

                val value = rawValues.toMutableList()
                value.removeAt(soc)
                value.removeAt(s1c - 1)
                value.removeAt(sou - 2)
                value.removeAt(s1u - 3)
                value.removeAt(ec - 4)
                value.removeAt(eu - 5)
                value.removeAt(ygc - 6)
                value.removeAt(ygct - 7)
                value.removeAt(fgc - 8)
                value.removeAt(fgct - 9)
                return Pair(headers.toList(), value.toList())
            }
        }
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

    private fun totalCapacity(typeOfCollector: TypeCollector, jspMapValues: Map<String, Double>): Double {
        if(typeOfCollector == TypeCollector.Z) {
            return jspMapValues["OC"]!! + jspMapValues["MC"]!!
        } else {
            return jspMapValues["EC"]!! + jspMapValues["OC"]!! + jspMapValues["S0C"]!! + jspMapValues["S1C"]!!
        }
    }

    private fun usage(typeOfCollector: TypeCollector, jspMapValues: Map<String, Double>): Double {
        if(typeOfCollector == TypeCollector.Z) {
            return jspMapValues["OU"]!! + jspMapValues["MU"]!!
        } else {
            return jspMapValues["S0U"]!! + jspMapValues["S1U"]!! + jspMapValues["EU"]!! + jspMapValues["OU"]!!
        }
    }

    private fun gcTime(jspMapValues: Map<String, Double>): Double {
        return jspMapValues["GCT"]!!
    }

    private fun uptime(jspMapValues: Map<String, Double>): Double {
        return jspMapValues["Timestamp"]!!
    }
}

enum class TypeCollector {
    G1,
    PARALLEL,
    Z
}
