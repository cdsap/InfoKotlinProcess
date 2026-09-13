package io.github.cdsap.kotlinprocess

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.build.event.BuildEventsListenerRegistry
import org.gradle.kotlin.dsl.support.serviceOf

class InfoKotlinProcessPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.gradle.rootProject {
            // Prefer the Develocity extension when present (settings-applied Develocity
            // installs it on the root project but does not set project.hasPlugin).
            // Never gate console output on Class.forName — a transitive jar without an
            // applied plugin must not produce a silent no-op.
            val scanConfigured =
                try {
                    DevelocityWrapperConfiguration().configureProjectWithDevelocityIfPresent(target)
                } catch (_: NoClassDefFoundError) {
                    false
                } catch (_: ExceptionInInitializerError) {
                    false
                }

            if (scanConfigured) {
                return@rootProject
            }

            target.pluginManager.withPlugin("com.gradle.develocity") {
                DevelocityWrapperConfiguration().configureProjectWithDevelocity(target)
            }

            if (!target.pluginManager.hasPlugin("com.gradle.develocity")) {
                consoleReporting(target)
            }
        }
    }

    private fun consoleReporting(project: Project) {
        val providers = project.kotlinProcessProviders()
        val service =
            project.gradle.sharedServices.registerIfAbsent(
                "kotlinProcessService",
                InfoKotlinProcessBuildService::class.java,
            ) {
                parameters.jInfoProvider = providers.jInfo
                parameters.jStatProvider = providers.jStat
            }
        project.serviceOf<BuildEventsListenerRegistry>().onTaskCompletion(service)
    }
}
