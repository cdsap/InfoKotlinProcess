package io.github.cdsap.kotlinprocess

import org.gradle.api.Project
import org.gradle.build.event.BuildEventsListenerRegistry
import org.gradle.kotlin.dsl.support.serviceOf

internal class KotlinProcessReporting {
    fun configure(
        target: Project,
        extension: InfoKotlinProcessExtension,
    ) {
        val rootProject = target.rootProject
        if (rootProject.extensions.findByName(CONFIGURED_MARKER) != null) {
            return
        }
        rootProject.extensions.add(CONFIGURED_MARKER, Any())

        // Prefer the Develocity extension when present (settings-applied Develocity
        // installs it on the root project but does not set project.hasPlugin).
        // Never gate console output on Class.forName — a transitive jar without an
        // applied plugin must not produce a silent no-op.
        val scanConfigured =
            try {
                DevelocityWrapperConfiguration()
                    .configureProjectWithDevelocityIfPresent(
                        target,
                        extension.gbos.json,
                        extension.gbos.ndjson,
                        extension.gbos.develocity,
                        extension.legacy.enabled,
                    )
            } catch (_: NoClassDefFoundError) {
                false
            } catch (_: ExceptionInInitializerError) {
                false
            }

        if (scanConfigured) {
            return
        }

        target.pluginManager.withPlugin("com.gradle.develocity") {
            DevelocityWrapperConfiguration()
                .configureProjectWithDevelocity(
                    target,
                    extension.gbos.json,
                    extension.gbos.ndjson,
                    extension.gbos.develocity,
                    extension.legacy.enabled,
                )
        }

        if (!target.pluginManager.hasPlugin("com.gradle.develocity")) {
            configureConsole(target, extension)
        }
    }

    private fun configureConsole(
        project: Project,
        extension: InfoKotlinProcessExtension,
    ) {
        val providers = project.kotlinProcessProviders()
        val service =
            project.gradle.sharedServices.registerIfAbsent(
                "kotlinProcessService",
                InfoKotlinProcessBuildService::class.java,
            ) {
                parameters.jInfoProvider = providers.jInfo
                parameters.jStatProvider = providers.jStat
                parameters.gbosJsonOutput.set(extension.gbos.json)
                parameters.gbosNdjsonOutput.set(extension.gbos.ndjson)
                parameters.gbosJsonFile.set(project.layout.buildDirectory.file("info-kotlin-process/gbos.json"))
                parameters.gbosNdjsonFile.set(project.layout.buildDirectory.file("info-kotlin-process/gbos.ndjson"))
            }
        project.serviceOf<BuildEventsListenerRegistry>().onTaskCompletion(service)
    }

    private companion object {
        const val CONFIGURED_MARKER = "infoKotlinProcessConfigured"
    }
}
