package io.github.cdsap.kotlinprocess

import io.github.cdsap.valuesourceprocess.jInfo
import io.github.cdsap.valuesourceprocess.jStat
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
        val service =
            project.gradle.sharedServices.registerIfAbsent(
                "kotlinProcessService",
                InfoKotlinProcessBuildService::class.java,
            ) {
                parameters.jInfoProvider = project.jInfo(Constants.KOTLIN_PROCESS_NAME)
                parameters.jStatProvider = project.jStat(Constants.KOTLIN_PROCESS_NAME)
            }
        project.serviceOf<BuildEventsListenerRegistry>().onTaskCompletion(service)
    }
}
