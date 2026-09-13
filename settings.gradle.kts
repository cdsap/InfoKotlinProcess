pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        exclusiveContent {
            forRepository { gradlePluginPortal() }
            filter { includeGroupByRegex("com\\.gradle.*") }
        }
        mavenCentral()
    }
}

// Root is build-wide configuration only; sources live in :plugin.
// Keep the published artifact name as "infokotlinprocess" (legacy classpath coordinate).
rootProject.name = "InfoKotlinProcess"
include("plugin")
project(":plugin").name = "infokotlinprocess"
