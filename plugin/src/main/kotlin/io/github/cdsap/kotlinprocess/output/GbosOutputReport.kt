package io.github.cdsap.kotlinprocess.output

import io.github.cdsap.jdk.tools.parser.model.Process
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal class GbosOutputReport(
    private val outputJson: File,
    private val outputNdjson: File,
) {
    fun write(
        processes: List<Process>,
        enabled: Boolean,
    ) {
        if (!enabled || processes.isEmpty()) {
            outputJson.delete()
            outputNdjson.delete()
            return
        }

        // Generate every sink before replacing an existing file. A bad observation
        // must not leave one sink from this build beside another sink from the previous one.
        val contents =
            listOf(
                outputJson to GbosDevelocityProjection.reportJson(processes),
                outputNdjson to GbosDevelocityProjection.ndjson(processes),
            )
        contents.forEach { (output, content) -> replaceAtomically(output, content) }
    }

    private fun replaceAtomically(
        output: File,
        content: String,
    ) {
        output.parentFile?.mkdirs()
        val temporary =
            Files.createTempFile(output.parentFile.toPath(), "${output.name}.", ".tmp").toFile()
        try {
            temporary.writeText(content)
            Files.move(
                temporary.toPath(),
                output.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } finally {
            temporary.delete()
        }
    }
}
