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

rootProject.name = "infokotlinprocess"
