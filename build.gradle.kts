plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
}

group = "io.github.cdsap.value"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}


publishing {
    publications {
        create<MavenPublication>("libs") {
            from(components["java"])
            artifactId = "values"
        }
    }
}
