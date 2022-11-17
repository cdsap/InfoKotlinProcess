package io.github.cdsap.valuesource.commandline

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import kotlin.math.pow
import kotlin.math.roundToInt

class InfoKotlinProcessPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val jstat =
            target.execute("jps | grep KotlinCompileDaemon | sed 's/KotlinCompileDaemon//' | while read ln; do  jstat -gc -t \$ln; echo \"\$ln\"; done")
        val jinfo =
            target.execute("jps | grep KotlinCompileDaemon | sed 's/KotlinCompileDaemon//' | while read ln; do  jinfo \$ln  | grep \"XX:MaxHeapSize\"; echo \"\$ln\";  done")
        target.gradle.rootProject {

            val extension = extensions.findByType(com.gradle.scan.plugin.BuildScanExtension::class.java)
            if (extension != null) {
                val buildScanExtension = extensions.getByType(com.gradle.scan.plugin.BuildScanExtension::class.java)
                buildScanExtension.buildFinished {
                    val k = ConsolidateProcesses().consolidate(jstat.get(), jinfo.get())
                    k.map {
                        buildScanExtension.value(
                            "Kotlin-Process-${it.pid}-max",
                            "${it.jInfo.max.toGigs()} GB"
                        )
                        buildScanExtension.value(
                            "Kotlin-Process-${it.pid}-usage",
                            "${it.jstatData.usage.toGigs()} GB"
                        )
                        buildScanExtension.value(
                            "Kotlin-Process-${it.pid}-capacity",
                            "${it.jstatData.capacity.toGigs()} GB"
                        )
                        buildScanExtension.value(
                            "Kotlin-Process-${it.pid}-uptime",
                            "${it.jstatData.uptime.toMinutes()} minutes"
                        )
                        buildScanExtension.value(
                            "Kotlin-Process-${it.pid}-gcTime",
                            "${it.jstatData.gcTime.toMinutes()} minutes"
                        )

                    }
                }
            }
        }
    }
}

fun Project.execute(command: String): Provider<String> {
    return providers.of(CommandLineWithOutputValue::class.java) {
        parameters.commands.set(command)
    }
}

fun Double.roundTo(numFractionDigits: Int): Double {
    val factor = 10.0.pow(numFractionDigits.toDouble())
    return (this * factor).roundToInt() / factor
}

fun Double.toGigs(): Double {
    return (this / 1048576).roundTo(2)
}

fun Double.toMinutes(): Double {
    return (this / 60).roundTo(2)
}
