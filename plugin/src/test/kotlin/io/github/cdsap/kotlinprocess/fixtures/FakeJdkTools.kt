package io.github.cdsap.kotlinprocess.fixtures

import java.io.File

/**
 * Deterministic stand-ins for `jps`, `jstat` and `jinfo`.
 *
 * The plugin's value sources (commandline-value-source) run
 * `sh -c "jps | grep KotlinCompileDaemon | ... jstat -gc -t $pid ..."` and
 * `sh -c "jps | grep KotlinCompileDaemon | ... jinfo $pid | grep XX:MaxHeapSize ..."`,
 * resolving the tools through `PATH`. Prepending [install]'s directory to the build's
 * `PATH` therefore feeds fixed output through the real value sources, collector and
 * reporting code without any production seam.
 */
data class FakeKotlinDaemon(
    val pid: String,
    val jStatHeader: String,
    val jStatData: String,
    val jInfoFlags: String,
)

object FakeJdkTools {
    const val JSTAT_HEADER =
        "Timestamp         S0C         S1C         S0U         S1U          EC          EU          OC          OU" +
            "          MC          MU        CCSC       CCSU     YGC     YGCT     FGC    FGCT     CGC    CGCT       GCT"

    val G1_DAEMON =
        FakeKotlinDaemon(
            pid = "12345",
            jStatHeader = JSTAT_HEADER,
            jStatData =
                "        1117.8          0.0      30720.0          0.0      30720.0    1135616.0     755712.0" +
                    "     865280.0     546816.0     195184.0    189433.3      22208.0    20357.8     22    0.682" +
                    "     0    0.000      12    0.070     0.752",
            jInfoFlags =
                "-XX:CICompilerCount=4 -XX:InitialHeapSize=268435456 -XX:MaxHeapSize=4294967296 " +
                    "-XX:+UseCompressedOops -XX:+UseG1GC",
        )

    val PARALLEL_DAEMON =
        FakeKotlinDaemon(
            pid = "23456",
            jStatHeader = JSTAT_HEADER,
            jStatData =
                "        3600.0      10240.0      10240.0          0.0       5120.0     524288.0     262144.0" +
                    "    1048576.0     786432.0     131072.0    129024.0      16384.0    15360.0     40    1.500" +
                    "     2    0.300       0    0.000    120.000",
            jInfoFlags = "-XX:MaxHeapSize=2147483648 -XX:+UseParallelGC",
        )

    /** Raw `jStat` value-source output for [daemons], as produced by the shell pipeline. */
    fun jStatOutput(daemons: List<FakeKotlinDaemon>): String =
        daemons.joinToString(separator = "") { "${it.jStatHeader}\n${it.jStatData}\n${it.pid}\n" }

    /** Raw `jInfo` value-source output for [daemons], as produced by the shell pipeline. */
    fun jInfoOutput(daemons: List<FakeKotlinDaemon>): String = daemons.joinToString(separator = "") { "${it.jInfoFlags}\n${it.pid}\n" }

    /** Writes executable `jps`, `jstat` and `jinfo` scripts into [binDir] and returns it. */
    fun install(
        binDir: File,
        daemons: List<FakeKotlinDaemon>,
    ): File {
        binDir.mkdirs()
        script(
            File(binDir, "jps"),
            buildString {
                appendLine("echo '4242 GradleDaemon'")
                daemons.forEach { appendLine("echo '${it.pid} KotlinCompileDaemon'") }
                appendLine("echo '777 Jps'")
            },
        )
        script(
            File(binDir, "jstat"),
            caseOnPid("\$3", daemons) { "${it.jStatHeader}\n${it.jStatData}" },
        )
        script(
            File(binDir, "jinfo"),
            caseOnPid("\$1", daemons) { "VM Flags:\n${it.jInfoFlags}" },
        )
        return binDir
    }

    private fun caseOnPid(
        pidArgument: String,
        daemons: List<FakeKotlinDaemon>,
        output: (FakeKotlinDaemon) -> String,
    ): String =
        buildString {
            appendLine("case \"$pidArgument\" in")
            daemons.forEach {
                appendLine("  ${it.pid})")
                appendLine("    cat <<'EOF'")
                appendLine(output(it))
                appendLine("EOF")
                appendLine("    ;;")
            }
            appendLine("esac")
        }

    private fun script(
        file: File,
        body: String,
    ) {
        file.writeText("#!/bin/sh\n$body")
        check(file.setExecutable(true)) { "Could not make $file executable" }
    }
}
