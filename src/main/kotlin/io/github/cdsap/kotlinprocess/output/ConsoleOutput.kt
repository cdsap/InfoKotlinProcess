package io.github.cdsap.kotlinprocess.output

import com.jakewharton.picnic.TextAlignment
import com.jakewharton.picnic.table
import io.github.cdsap.kotlinprocess.model.Process
import io.github.cdsap.kotlinprocess.toMinutes

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
                            columnSpan = 6
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
                            cell("${it.max} Gb")
                            cell("${it.usage} Gb")
                            cell("${it.capacity} Gb")
                            cell("${it.gcTime} minutes")
                            cell("${it.uptime} minutes")
                        }
                    }
                }
            })
    }
}
