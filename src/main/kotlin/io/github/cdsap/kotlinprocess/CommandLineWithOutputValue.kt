package io.github.cdsap.valuesource.commandline

import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations
import org.gradle.process.internal.ExecException
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import javax.inject.Inject

abstract class CommandLineWithOutputValue : ValueSource<String, CommandLineWithOutputValue.Parameters> {
    interface Parameters : ValueSourceParameters {
        val commands: Property<String>
    }

    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): String {
        val output = ByteArrayOutputStream()
        val error = ByteArrayOutputStream()
        return try {
            execOperations.exec {
                try {
                    commandLine("sh", "-c", parameters.commands.get())
                    standardOutput = output
                    errorOutput = error
                } catch (e: Exception) {
                }
            }
            String(output.toByteArray(), Charset.defaultCharset())
        } catch (e: ExecException) {
            ""
        }
    }

}
