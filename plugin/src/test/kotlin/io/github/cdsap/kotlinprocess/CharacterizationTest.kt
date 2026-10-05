package io.github.cdsap.kotlinprocess

import groovy.json.JsonSlurper
import io.github.cdsap.jdk.tools.parser.model.Process
import io.github.cdsap.jdk.tools.parser.model.TypeProcess
import io.github.cdsap.kotlinprocess.fixtures.FakeDevelocityPlugin
import io.github.cdsap.kotlinprocess.fixtures.FakeJdkTools
import io.github.cdsap.kotlinprocess.fixtures.FakeKotlinDaemon
import io.github.cdsap.kotlinprocess.output.ConsoleOutput
import io.github.cdsap.kotlinprocess.output.DevelocityCustomValues
import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

/**
 * Characterization (golden) tests pinning what consumers of the plugin observe today:
 * build scan custom values (keys, order, value formats) and the console table.
 *
 * Process input is deterministic everywhere:
 * - Unit level: fixture `jstat`/`jinfo` value-source output fed to the real
 *   [KotlinProcessCollector] and the real reporting code.
 * - TestKit level: [FakeJdkTools] puts fake `jps`/`jstat`/`jinfo` scripts first on the build's
 *   `PATH`, so the real commandline value sources, collector, build service and
 *   `DevelocityValues` run against fixed output. No real Kotlin daemon is involved.
 *
 * Build scan values are observed through [FakeDevelocityPlugin], a settings plugin standing in
 * for Develocity 4.x, which prints `SCAN-VALUE <name>=<value>` for each `buildScan.value` call.
 * Real Develocity 4.x refuses to be applied to a project, so only settings-applied Develocity
 * is characterized.
 */
class CharacterizationTest {
    @Rule
    @JvmField
    val temporaryFolder = TemporaryFolder()

    @Test
    fun oneKotlinDaemonFixtureIsCollectedWithExactFields() {
        assertEquals(
            listOf(
                Process("12345", 4.0, 1.27, 1.94, 0.01, 18.63, "-XX:+UseG1GC", TypeProcess.Kotlin),
            ),
            collect(FakeJdkTools.G1_DAEMON),
        )
    }

    @Test
    fun oneKotlinDaemonEmitsExactlySixLegacyValuesInOrder() {
        assertEquals(
            G1_LEGACY_VALUES,
            DevelocityCustomValues
                .from(collect(FakeJdkTools.G1_DAEMON), gbosEnabled = false)
                .map { (name, value) -> "$name=$value" },
        )
    }

    @Test
    fun severalKotlinDaemonsEmitLegacyValuesGroupedPerProcessInJstatOrder() {
        assertEquals(
            G1_LEGACY_VALUES +
                listOf(
                    "Kotlin-Process-23456-max=2.0 GB",
                    "Kotlin-Process-23456-usage=1.0 GB",
                    "Kotlin-Process-23456-capacity=1.52 GB",
                    "Kotlin-Process-23456-uptime=60.0 minutes",
                    "Kotlin-Process-23456-gcTime=2.0 minutes",
                    "Kotlin-Process-23456-gcType=-XX:+UseParallelGC",
                ),
            DevelocityCustomValues
                .from(collect(FakeJdkTools.G1_DAEMON, FakeJdkTools.PARALLEL_DAEMON), gbosEnabled = false)
                .map { (name, value) -> "$name=$value" },
        )
    }

    @Test
    fun gbosOptInReplacesLegacyValuesWithHeaderAndOneObservationPerDaemon() {
        val values =
            DevelocityCustomValues.from(
                collect(FakeJdkTools.G1_DAEMON, FakeJdkTools.PARALLEL_DAEMON),
                gbosEnabled = true,
            )

        assertEquals(
            GBOS_HEADER_KEYS + listOf(OBSERVATION_KEY, OBSERVATION_KEY),
            values.map { it.first },
        )
        assertEquals(GBOS_HEADER_VALUES, values.take(3).map { it.second })
        assertG1Observation(values[3].second)
        assertObservation(
            values[4].second,
            pid = 23456,
            gcName = "Parallel",
            heapLimitBytes = 2147483648.0,
            heapUsedBytes = 1073741824.0,
            heapCommittedBytes = 1632087572.0,
            gcTimeSeconds = 120.0,
            uptimeSeconds = 3600.0,
        )
    }

    @Test
    fun consoleTablePinsTitleHeaderColumnsAndRowFormat() {
        assertEquals(G1_CONSOLE_TABLE, captureConsole(collect(FakeJdkTools.G1_DAEMON)))
        assertEquals(
            """
            ┌───────────────────────────────────────────────────────────────────────────────────────────────────────┐
            │  Kotlin processes                                                                                     │
            ├─────────┬──────────┬───────────┬────────────┬────────────────┬──────────────────────┬─────────────────┤
            │  PID    │  Max     │  Usage    │  Capacity  │  GC Time       │  GC Type             │  Uptime         │
            ├─────────┼──────────┼───────────┼────────────┼────────────────┼──────────────────────┼─────────────────┤
            │  12345  │  4.0 Gb  │  1.27 Gb  │  1.94 Gb   │  0.01 minutes  │  -XX:+UseG1GC        │  18.63 minutes  │
            ├─────────┼──────────┼───────────┼────────────┼────────────────┼──────────────────────┼─────────────────┤
            │  23456  │  2.0 Gb  │  1.0 Gb   │  1.52 Gb   │  2.0 minutes   │  -XX:+UseParallelGC  │  60.0 minutes   │
            └─────────┴──────────┴───────────┴────────────┴────────────────┴──────────────────────┴─────────────────┘
            """.trimIndent(),
            captureConsole(collect(FakeJdkTools.G1_DAEMON, FakeJdkTools.PARALLEL_DAEMON)),
        )
    }

    @Test
    fun withoutDevelocityTheConsoleTableIsTheOnlyOutputAndIgnoresGbosOptIn() {
        val project = project("build.gradle" to "plugins { id 'io.github.cdsap.kotlinprocess' }")

        val legacy = build(project, develocity = false)
        val gbos = build(project, develocity = false, "-P$GBOS_PROPERTY=true")

        listOf(legacy, gbos).forEach { output ->
            assertEquals(output, listOf(G1_CONSOLE_TABLE), output.consoleTables())
            assertEquals(output, emptyList<String>(), output.scanValues())
        }
    }

    @Test
    fun settingsAppliedPluginWithoutDevelocityPrintsTheSameConsoleTable() {
        val project =
            project(
                "settings.gradle" to "plugins { id 'io.github.cdsap.kotlinprocess' }",
                "build.gradle" to "",
            )

        val output = build(project, develocity = false)

        assertEquals(output, listOf(G1_CONSOLE_TABLE), output.consoleTables())
    }

    @Test
    fun withoutKotlinDaemonNothingIsReported() {
        val withoutDevelocity = project("build.gradle" to "plugins { id 'io.github.cdsap.kotlinprocess' }")
        val withDevelocity = settingsProject(DEVELOCITY_THEN_PLUGIN)

        val consoleOutput = build(withoutDevelocity, develocity = false, daemons = emptyList())
        val scanOutput = build(withDevelocity, develocity = true, daemons = emptyList())

        assertEquals(consoleOutput, emptyList<String>(), consoleOutput.consoleTables())
        assertTrue(scanOutput, scanOutput.contains(FakeDevelocityPlugin.APPLIED_MARKER))
        assertEquals(scanOutput, emptyList<String>(), scanOutput.scanValues())
        assertEquals(scanOutput, emptyList<String>(), scanOutput.consoleTables())
    }

    @Test
    fun settingsDevelocityWithSettingsPluginReportsLegacyValuesToBuildScanOnly() {
        val output = build(settingsProject(DEVELOCITY_THEN_PLUGIN), develocity = true)

        assertTrue(output, output.contains(FakeDevelocityPlugin.APPLIED_MARKER))
        assertEquals(output, G1_LEGACY_VALUES, output.scanValues())
        assertEquals(output, emptyList<String>(), output.consoleTables())
    }

    @Test
    fun settingsPluginOrderDoesNotChangeBuildScanValuesOrConsoleOutput() {
        val before = build(settingsProject(DEVELOCITY_THEN_PLUGIN), develocity = true)
        val after = build(settingsProject(PLUGIN_THEN_DEVELOCITY), develocity = true)

        assertEquals(before, G1_LEGACY_VALUES, before.scanValues())
        assertEquals(after, before.scanValues(), after.scanValues())

        // The plugin's settings path resolves Develocity at projectsLoaded, after Develocity has
        // put its extension on the root project, so neither order falls back to the console.
        assertEquals(before, emptyList<String>(), before.consoleTables())
        assertEquals(after, emptyList<String>(), after.consoleTables())
    }

    @Test
    fun settingsDevelocityWithPluginAppliedFromRootBuildScriptReportsToBuildScanOnly() {
        // Legacy build-script use: the project path runs configureProject immediately (the root
        // project is already available) and finds the root-project `develocity` extension that
        // the settings plugin installed, so it reports to the build scan and skips the console.
        val output =
            build(
                project(
                    "settings.gradle" to "plugins { id 'com.gradle.develocity' }",
                    "build.gradle" to "plugins { id 'io.github.cdsap.kotlinprocess' }",
                ),
                develocity = true,
            )

        assertTrue(output, output.contains(FakeDevelocityPlugin.APPLIED_MARKER))
        assertEquals(output, G1_LEGACY_VALUES, output.scanValues())
        assertEquals(output, emptyList<String>(), output.consoleTables())
    }

    @Test
    fun gbosGradlePropertyReplacesLegacyBuildScanValues() {
        val output =
            build(
                settingsProject(DEVELOCITY_THEN_PLUGIN),
                develocity = true,
                "-P$GBOS_PROPERTY=true",
            )
        val values = output.scanValues().map { it.substringBefore('=') to it.substringAfter('=') }

        assertEquals(output, GBOS_HEADER_KEYS + OBSERVATION_KEY, values.map { it.first })
        assertEquals(output, GBOS_HEADER_VALUES, values.take(3).map { it.second })
        assertG1Observation(values[3].second)
        assertEquals(output, emptyList<String>(), output.consoleTables())
    }

    @Test
    fun configurationCacheReuseEmitsTheSameConsoleTable() {
        val project = project("build.gradle" to "plugins { id 'io.github.cdsap.kotlinprocess' }")

        val (store, reuse) = storeAndReuseConfigurationCache(project, develocity = false)

        assertEquals(store, listOf(G1_CONSOLE_TABLE), store.consoleTables())
        assertEquals(reuse, listOf(G1_CONSOLE_TABLE), reuse.consoleTables())
    }

    @Test
    fun configurationCacheReuseEmitsTheSameBuildScanValues() {
        val project = settingsProject(DEVELOCITY_THEN_PLUGIN)

        val (store, reuse) = storeAndReuseConfigurationCache(project, develocity = true)

        assertTrue(store, store.contains(FakeDevelocityPlugin.APPLIED_MARKER))
        assertFalse(
            "Reuse must replay buildFinished actions from the cache entry without configuring:\n$reuse",
            reuse.contains(FakeDevelocityPlugin.APPLIED_MARKER),
        )
        assertEquals(store, G1_LEGACY_VALUES, store.scanValues())
        assertEquals(reuse, G1_LEGACY_VALUES, reuse.scanValues())
        assertEquals(reuse, emptyList<String>(), reuse.consoleTables())
    }

    private fun collect(vararg daemons: FakeKotlinDaemon): List<Process> =
        KotlinProcessCollector().collect(
            FakeJdkTools.jStatOutput(daemons.toList()),
            FakeJdkTools.jInfoOutput(daemons.toList()),
        )

    private fun captureConsole(processes: List<Process>): String {
        val original = System.out
        val captured = ByteArrayOutputStream()
        System.setOut(PrintStream(captured, true, Charsets.UTF_8.name()))
        try {
            ConsoleOutput(processes).print()
        } finally {
            System.setOut(original)
        }
        return captured.toString(Charsets.UTF_8.name()).trimEnd()
    }

    private fun assertG1Observation(json: String) =
        assertObservation(
            json,
            pid = 12345,
            gcName = "G1",
            heapLimitBytes = 4294967296.0,
            heapUsedBytes = 1363652116.0,
            heapCommittedBytes = 2083059139.0,
            gcTimeSeconds = 0.6,
            uptimeSeconds = 1117.8,
        )

    /** Asserts parsed structure and numeric values, not the raw JSON rendering. */
    private fun assertObservation(
        json: String,
        pid: Long,
        gcName: String,
        heapLimitBytes: Double,
        heapUsedBytes: Double,
        heapCommittedBytes: Double,
        gcTimeSeconds: Double,
        uptimeSeconds: Double,
    ) {
        @Suppress("UNCHECKED_CAST")
        val observation = JsonSlurper().parseText(json) as Map<String, Any?>
        assertEquals(json, setOf("scope", "aggregationScope", "attributes", "measurements"), observation.keys)
        assertEquals(json, "jvm.process", observation["scope"])
        assertEquals(json, "entity", observation["aggregationScope"])

        @Suppress("UNCHECKED_CAST")
        val attributes = observation["attributes"] as Map<String, Any?>
        assertEquals(json, setOf("process.pid", "jvm.process.role", "jvm.gc.name"), attributes.keys)
        assertEquals(json, pid, (attributes["process.pid"] as Number).toLong())
        assertEquals(json, "kotlin-daemon", attributes["jvm.process.role"])
        assertEquals(json, gcName, attributes["jvm.gc.name"])

        @Suppress("UNCHECKED_CAST")
        val measurements = observation["measurements"] as List<Map<String, Any?>>
        val expected =
            listOf(
                Measurement("jvm.process.memory.heap.limit", heapLimitBytes, "By", "last"),
                Measurement("jvm.process.memory.heap.used", heapUsedBytes, "By", "last"),
                Measurement("jvm.process.memory.heap.committed", heapCommittedBytes, "By", "last"),
                Measurement("jvm.process.gc.time", gcTimeSeconds, "s", "sum"),
                Measurement("jvm.process.uptime", uptimeSeconds, "s", "last"),
            )
        assertEquals(json, expected.size, measurements.size)
        expected.zip(measurements).forEach { (measurement, actual) ->
            assertEquals(json, setOf("name", "value", "unit", "aggregation"), actual.keys)
            assertEquals(json, measurement.name, actual["name"])
            assertEquals(json, measurement.value, (actual["value"] as Number).toDouble(), 1e-6)
            assertEquals(json, measurement.unit, actual["unit"])
            assertEquals(json, measurement.aggregation, actual["aggregation"])
        }
    }

    private data class Measurement(
        val name: String,
        val value: Double,
        val unit: String,
        val aggregation: String,
    )

    private fun project(vararg files: Pair<String, String>): File {
        val directory = temporaryFolder.newFolder()
        if (files.none { it.first == "settings.gradle" }) {
            File(directory, "settings.gradle").writeText("")
        }
        files.forEach { (name, content) -> File(directory, name).writeText(content) }
        return directory
    }

    private fun settingsProject(settingsPlugins: String): File = project("settings.gradle" to settingsPlugins, "build.gradle" to "")

    private fun storeAndReuseConfigurationCache(
        project: File,
        develocity: Boolean,
    ): Pair<String, String> {
        val store = build(project, develocity, "--configuration-cache")
        val reuse = build(project, develocity, "--configuration-cache")
        assertTrue(store, store.contains("Configuration cache entry stored."))
        assertTrue(reuse, reuse.contains("Reusing configuration cache."))
        return store to reuse
    }

    private fun build(
        project: File,
        develocity: Boolean,
        vararg arguments: String,
        daemons: List<FakeKotlinDaemon> = listOf(FakeJdkTools.G1_DAEMON),
    ): String {
        val fakeToolsDirectory = FakeJdkTools.install(temporaryFolder.newFolder(), daemons)
        return GradleRunner
            .create()
            .withProjectDir(project)
            .withArguments(listOf("help") + arguments)
            .withPluginClasspath(if (develocity) pluginClasspathWithFakeDevelocity() else defaultPluginClasspath())
            .withEnvironment(
                System.getenv() + ("PATH" to "${fakeToolsDirectory.absolutePath}${File.pathSeparator}${System.getenv("PATH")}"),
            ).withGradleVersion(GRADLE_VERSION)
            .build()
            .output
    }

    private fun defaultPluginClasspath(): List<File> =
        GradleRunner
            .create()
            .withPluginClasspath()
            .pluginClasspath
            .toList()

    /**
     * The test resources directory goes first so its `com.gradle.develocity` plugin descriptor
     * shadows the one inside the real Develocity jar, which is only needed for the API
     * interfaces the fake implements.
     */
    private fun pluginClasspathWithFakeDevelocity(): List<File> {
        val descriptor =
            requireNotNull(javaClass.classLoader.getResource(FAKE_DEVELOCITY_DESCRIPTOR)) {
                "Missing $FAKE_DEVELOCITY_DESCRIPTOR on the test classpath"
            }
        val testResources = File(descriptor.toURI()).parentFile.parentFile.parentFile
        val testClasses =
            File(
                FakeDevelocityPlugin::class.java.protectionDomain.codeSource.location
                    .toURI(),
            )
        val develocityApiJar =
            File(
                System.getProperty("develocity.probe.classpath.jar")
                    ?: error("Missing develocity.probe.classpath.jar system property"),
            )
        return listOf(testResources, testClasses) + defaultPluginClasspath() + develocityApiJar
    }

    private fun String.scanValues(): List<String> =
        lines()
            .filter { it.startsWith("SCAN-VALUE ") }
            .map { it.removePrefix("SCAN-VALUE ") }

    private fun String.consoleTables(): List<String> {
        val lines = lines()
        return lines.indices
            .filter { lines[it].startsWith("┌") }
            .map { start ->
                val end = (start until lines.size).first { lines[it].startsWith("└") }
                lines.subList(start, end + 1).joinToString("\n")
            }
    }

    private companion object {
        const val GRADLE_VERSION = "8.14.2"
        const val GBOS_PROPERTY = "infoKotlinProcess.gbos.develocity.enabled"
        const val FAKE_DEVELOCITY_DESCRIPTOR = "META-INF/gradle-plugins/com.gradle.develocity.properties"
        const val OBSERVATION_KEY = "gbos.v1.producer.info_kotlin_process.observation"

        val DEVELOCITY_THEN_PLUGIN =
            """
            plugins {
                id 'com.gradle.develocity'
                id 'io.github.cdsap.kotlinprocess'
            }
            """.trimIndent()

        val PLUGIN_THEN_DEVELOCITY =
            """
            plugins {
                id 'io.github.cdsap.kotlinprocess'
                id 'com.gradle.develocity'
            }
            """.trimIndent()

        val G1_LEGACY_VALUES =
            listOf(
                "Kotlin-Process-12345-max=4.0 GB",
                "Kotlin-Process-12345-usage=1.27 GB",
                "Kotlin-Process-12345-capacity=1.94 GB",
                "Kotlin-Process-12345-uptime=18.63 minutes",
                "Kotlin-Process-12345-gcTime=0.01 minutes",
                "Kotlin-Process-12345-gcType=-XX:+UseG1GC",
            )

        val GBOS_HEADER_KEYS =
            listOf(
                "gbos.schema",
                "gbos.v1.producer.info_kotlin_process.name",
                "gbos.v1.producer.info_kotlin_process.version",
            )

        val GBOS_HEADER_VALUES = listOf("1.0.0", "info-kotlin-process", "0.0.4")

        val G1_CONSOLE_TABLE =
            """
            ┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
            │  Kotlin processes                                                                               │
            ├─────────┬──────────┬───────────┬────────────┬────────────────┬────────────────┬─────────────────┤
            │  PID    │  Max     │  Usage    │  Capacity  │  GC Time       │  GC Type       │  Uptime         │
            ├─────────┼──────────┼───────────┼────────────┼────────────────┼────────────────┼─────────────────┤
            │  12345  │  4.0 Gb  │  1.27 Gb  │  1.94 Gb   │  0.01 minutes  │  -XX:+UseG1GC  │  18.63 minutes  │
            └─────────┴──────────┴───────────┴────────────┴────────────────┴────────────────┴─────────────────┘
            """.trimIndent()
    }
}
