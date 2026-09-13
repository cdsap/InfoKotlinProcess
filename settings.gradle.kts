pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

// Root is build-wide configuration only; sources live in :plugin.
// Keep the published artifact name as "infokotlinprocess" (legacy classpath coordinate).
rootProject.name = "InfoKotlinProcess"
include("plugin")
project(":plugin").name = "infokotlinprocess"
