package io.github.cdsap.kotlinprocess

import org.gradle.api.Project

internal class GbosOutputFiles(
    project: Project,
) {
    val jsonFile =
        project.layout.buildDirectory.file(
            "info-kotlin-process/gbos.json",
        )
    val ndjsonFile =
        project.layout.buildDirectory.file(
            "info-kotlin-process/gbos.ndjson",
        )
}
