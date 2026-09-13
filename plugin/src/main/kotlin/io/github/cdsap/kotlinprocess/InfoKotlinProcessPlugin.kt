package io.github.cdsap.kotlinprocess

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.build.event.BuildEventsListenerRegistry
import org.gradle.kotlin.dsl.support.serviceOf

class InfoKotlinProcessPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.gradle.rootProject {
            val hasDevelocity =
                try {
                    Class.forName("com.gradle.develocity.agent.gradle.DevelocityConfiguration")
                    true
                } catch (_: ClassNotFoundException) {
                    false
                }
            if (hasDevelocity) {
                DevelocityWrapperConfiguration().configureProjectWithDevelocity(target)
            } else {
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
