package io.github.cdsap.kotlinprocess.output.console

import com.jakewharton.picnic.TextAlignment
import com.jakewharton.picnic.table
import io.github.cdsap.kotlinprocess.model.Process

class ConsoleOutput(private val processes: List<Process>) {
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
                            columnSpan = 5
                        }
                    }
                    row {
                        cell("PID")
                        cell("Max")
                        cell("Usage")
                        cell("Capacity")
                        cell("GC Time")
                        cell("Uptime")
                    }

                    processes.forEach {
                        row {
                            cell(it.pid)
                            cell("${it.jInfo.max} Gb")
                            cell("${it.jstatData.usage} Gb")
                            cell("${it.jstatData.capacity} Gb")
                            cell("${it.jstatData.gcTime} minutes")
                            cell("${it.jstatData.uptime} minutes")
                        }
                    }
                }

            })
    }
}
