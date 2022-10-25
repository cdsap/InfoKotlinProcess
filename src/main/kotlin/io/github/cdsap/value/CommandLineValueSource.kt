package io.github.cdsap.value

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class CommandLineValueSource : ValueSource<String, CommandLineValueSource.Parameters> {
    interface Parameters : ValueSourceParameters {
        val commands: ListProperty<String>
    }

    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): String {
        return execOperations.exec {
            commandLine(parameters.commands.get())
            isIgnoreExitValue = true
        }.exitValue.toString()
    }
}
