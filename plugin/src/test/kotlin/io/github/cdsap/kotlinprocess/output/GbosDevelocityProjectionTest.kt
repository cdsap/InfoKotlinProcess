package io.github.cdsap.kotlinprocess.output

import com.networknt.schema.InputFormat
import com.networknt.schema.SchemaLocation
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SpecificationVersion
import groovy.json.JsonOutput
import io.github.cdsap.jdk.tools.parser.model.Process
import io.github.cdsap.jdk.tools.parser.model.TypeProcess
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

class GbosDevelocityProjectionTest {
    private val process =
        Process(
            "9011",
            1.0,
            0.5,
            0.75,
            0.25,
            2.5,
            "-XX:+UseG1GC",
            TypeProcess.Kotlin,
        )

    @Test
    fun gbosOptInEmitsHeaderAndAStandardizedKotlinDaemonObservation() {
        val values =
            DevelocityCustomValues
                .from(listOf(process), gbosEnabled = true)
                .toMap()

        assertEquals("1.0.0", values["gbos.schema"])
        assertEquals("info-kotlin-process", values["gbos.v1.producer.info_kotlin_process.name"])
        assertEquals("0.0.4", values["gbos.v1.producer.info_kotlin_process.version"])

        val observation = values.getValue("gbos.v1.producer.info_kotlin_process.observation")
        assertTrue(observation.contains("\"scope\":\"jvm.process\""))
        assertTrue(observation.contains("\"aggregationScope\":\"entity\""))
        assertTrue(observation.contains("\"process.pid\":9011"))
        assertTrue(observation.contains("\"jvm.process.role\":\"kotlin-daemon\""))
        assertTrue(observation.contains("\"jvm.gc.name\":\"G1\""))
        assertTrue(observation.contains("\"value\":1073741824,\"unit\":\"By\""))
        assertTrue(observation.contains("\"value\":536870912,\"unit\":\"By\""))
        assertTrue(observation.contains("\"value\":15.0,\"unit\":\"s\""))
        assertTrue(observation.contains("\"value\":150.0,\"unit\":\"s\""))
    }

    @Test
    fun gbosAndLegacyBuildScanValuesAreMutuallyExclusive() {
        val gbosValues = DevelocityCustomValues.from(listOf(process), gbosEnabled = true)
        val legacyValues = DevelocityCustomValues.from(listOf(process), gbosEnabled = false)

        assertTrue(gbosValues.all { (name, _) -> name.startsWith("gbos.") })
        assertFalse(legacyValues.any { (name, _) -> name.startsWith("gbos.") })
        assertTrue(legacyValues.all { (name, _) -> name.startsWith("Kotlin-Process-") })
        assertTrue(legacyValues.any { (name, _) -> name.endsWith("-max") })
    }

    @Test
    fun outputValidatesAgainstThePublishedGbosSchemas() {
        val values = DevelocityCustomValues.from(listOf(process), gbosEnabled = true)
        val resources = javaClass.classLoader
        val schemaRegistry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
        val projectionSchema =
            schemaRegistry.getSchema(SchemaLocation.of("classpath:schema/develocity-projection.schema.json"))
        val fragmentSchema =
            schemaRegistry.getSchema(SchemaLocation.of("classpath:schema/observation-fragment.schema.json"))

        assertTrue(
            "the released GBOS contract must be available on the test classpath",
            resources.getResource("schema/report.schema.json") != null,
        )

        val projection =
            JsonOutput.toJson(
                linkedMapOf(
                    "schemaVersion" to GbosDevelocityProjection.SCHEMA_VERSION,
                    "customValues" to
                        values.map { (name, value) ->
                            linkedMapOf("name" to name, "value" to value)
                        },
                    "tags" to emptyList<String>(),
                ),
            )
        val projectionErrors = projectionSchema.validate(projection, InputFormat.JSON)
        assertTrue("GBOS Develocity projection is invalid: $projectionErrors", projectionErrors.isEmpty())

        val observations =
            values.filter { (name, _) -> name == GbosDevelocityProjection.OBSERVATION_VALUE }
        observations
            .forEach { (_, observation) ->
                val observationErrors = fragmentSchema.validate(observation, InputFormat.JSON)
                assertTrue("GBOS observation fragment is invalid: $observationErrors", observationErrors.isEmpty())
            }
    }
}
