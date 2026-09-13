package io.github.cdsap.kotlinprocess.output

import com.jakewharton.picnic.TextAlignment
import com.jakewharton.picnic.table
import io.github.cdsap.jdk.tools.parser.model.Process

class ConsoleOutput(
    private val processes: List<Process>,
) {
    fun print() {
        println(
            table {
                cellStyle {
                    border = true
                    alignment = TextAlignment.MiddleLeft
                    paddingLeft = 2
                    paddingRight = 2
                }
                body {
                    row {
                        cell("Kotlin processes") {
                            columnSpan = 7
                        }
                    }
                    row {
                        cell("PID")
                        cell("Max")
                        cell("Usage")
                        cell("Capacity")
                        cell("GC Time")
                        cell("GC Type")
                        cell("Uptime")
                    }

                    processes.forEach {
                        row {
                            cell(it.pid)
                            cell("${it.max} Gb")
                            cell("${it.usage} Gb")
                            cell("${it.capacity} Gb")
                            cell("${it.gcTime} minutes")
                            cell(it.typeGc)
                            cell("${it.uptime} minutes")
                        }
                    }
                }
            },
        )
    }
}
