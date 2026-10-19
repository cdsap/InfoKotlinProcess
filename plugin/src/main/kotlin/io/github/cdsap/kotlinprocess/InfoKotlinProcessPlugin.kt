package io.github.cdsap.kotlinprocess

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings

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
            val gbosEnabled = settingsExtension.gbos.enabled.get()
            val projectExtension =
                rootProject.extensions.findByType(InfoKotlinProcessExtension::class.java)
                    ?: rootProject.extensions.create(
                        "infoKotlinProcess",
                        InfoKotlinProcessExtension::class.java,
                    )
            projectExtension.gbos.enabled.convention(gbosEnabled)
            KotlinProcessReporting().configure(rootProject, projectExtension)
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
            KotlinProcessReporting().configure(target, extension)
        }
    }
}
