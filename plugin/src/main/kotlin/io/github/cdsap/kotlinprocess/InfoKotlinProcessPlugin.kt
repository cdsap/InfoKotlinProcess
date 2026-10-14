package io.github.cdsap.kotlinprocess

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.build.event.BuildEventsListenerRegistry
import org.gradle.kotlin.dsl.support.serviceOf

class InfoKotlinProcessPlugin : Plugin<Any> {
    override fun apply(target: Any) {
        when (target) {
            is Settings -> applyToSettings(target)
            is Project -> applyToProject(target)
            else -> throw GradleException("InfoKotlinProcessPlugin can only be applied to Settings or Project")
        }
    }

    private fun applyToSettings(settings: Settings) {
        val settingsExtension =
            settings.extensions.create(
                "infoKotlinProcess",
                InfoKotlinProcessExtension::class.java,
            )
        GbosOptIn.configureConventions(settingsExtension.gbos, settings.providers)

        // projectsLoaded runs after the settings script and before project evaluation,
        // so settings DSL values are final and the root extension is visible to build scripts.
        settings.gradle.projectsLoaded {
            val gbosJson = settingsExtension.gbos.json.get()
            val gbosNdjson = settingsExtension.gbos.ndjson.get()
            val projectExtension =
                rootProject.extensions.findByType(InfoKotlinProcessExtension::class.java)
                    ?: rootProject.extensions.create(
                        "infoKotlinProcess",
                        InfoKotlinProcessExtension::class.java,
                    )
            projectExtension.gbos.json.convention(gbosJson)
            projectExtension.gbos.ndjson.convention(gbosNdjson)
            configureProject(rootProject, projectExtension)
        }
    }

    private fun applyToProject(target: Project) {
        val rootProject = target.rootProject
        val extension =
            rootProject.extensions.findByType(InfoKotlinProcessExtension::class.java)
                ?: rootProject.extensions.create(
                    "infoKotlinProcess",
                    InfoKotlinProcessExtension::class.java,
                )
        GbosOptIn.configureConventions(extension.gbos, rootProject.providers)

        target.gradle.rootProject {
            configureProject(target, extension)
        }
    }

    companion object {
        private const val CONFIGURED_MARKER = "infoKotlinProcessConfigured"

        private fun configureProject(
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
                    .configureProjectWithDevelocity(target, extension.gbos.json, extension.gbos.ndjson)
            }

            if (!target.pluginManager.hasPlugin("com.gradle.develocity")) {
                processReporting(target, extension)
            }
        }

        private fun processReporting(
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
    }
}
