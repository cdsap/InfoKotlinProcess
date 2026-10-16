package io.github.cdsap.kotlinprocess.output

import com.networknt.schema.InputFormat
import com.networknt.schema.SchemaLocation
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SpecificationVersion
import io.github.cdsap.jdk.tools.parser.model.Process
import io.github.cdsap.jdk.tools.parser.model.TypeProcess
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GbosOutputReportTest {
    @Rule
    @JvmField
    val temporaryFolder = TemporaryFolder()

    private val process = Process("9011", 1.0, 0.5, 0.75, 0.25, 2.5, "-XX:+UseG1GC", TypeProcess.Kotlin)

    @Test
    fun writesStableJsonAndNdjsonFilesFromTheSameObservations() {
        val json = temporaryFolder.newFile("gbos.json")
        val ndjson = temporaryFolder.newFile("gbos.ndjson")

        GbosOutputReport(json, ndjson).write(listOf(process), enabled = true)

        assertTrue(json.readText().contains("\"schemaVersion\": \"1.0.0\""))
        assertTrue(json.readText().contains("\"scope\": \"jvm.process\""))
        assertEquals(1, ndjson.readLines().size)
        assertTrue(ndjson.readText().contains("\"scope\":\"jvm.process\""))
        assertFalse(json.readText().contains(".tmp"))

        val schema =
            SchemaRegistry
                .withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
                .getSchema(SchemaLocation.of("classpath:schema/report.schema.json"))
        assertTrue(schema.validate(json.readText(), InputFormat.JSON).isEmpty())
        val observationSchema =
            SchemaRegistry
                .withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
                .getSchema(SchemaLocation.of("classpath:schema/observation.schema.json"))
        ndjson.readLines().forEach { line ->
            assertTrue(observationSchema.validate(line, InputFormat.JSON).isEmpty())
        }
    }

    @Test
    fun failedGenerationPreservesExistingFiles() {
        val json = temporaryFolder.newFile("gbos.json").apply { writeText("old-json") }
        val ndjson = temporaryFolder.newFile("gbos.ndjson").apply { writeText("old-ndjson") }
        val invalid = process.copy(pid = "not-a-number")

        try {
            GbosOutputReport(json, ndjson).write(listOf(invalid), enabled = true)
        } catch (_: IllegalArgumentException) {
            // Expected: canonical observation generation validates the PID before any replacement.
        }

        assertEquals("old-json", json.readText())
        assertEquals("old-ndjson", ndjson.readText())
    }
}
