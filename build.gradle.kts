plugins {
    `java-gradle-plugin`
    `maven-publish`
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "1.0.0-rc-1"
}

group = "io.github.cdsap"
version = "0.2.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

dependencies {
    implementation("io.github.cdsap:jdk-tools-parser:0.1.1")
    implementation("io.github.cdsap:commandline-value-source:0.1.0")
    implementation("com.jakewharton.picnic:picnic:0.7.0")
    implementation("com.gradle:develocity-gradle-plugin:4.2.2")
    testImplementation("junit:junit:4.13.2")
}
tasks.withType<Test>().configureEach {
    filter {

        if (project.hasProperty("excludeTests")) {
            excludeTest(project.property("excludeTests").toString(),"")
        }
    }
}
gradlePlugin {
    plugins {
        create("InfoKotlinProcessPlugin") {
            id = "io.github.cdsap.kotlinprocess"
            displayName = "Info Kotlin Processes"
            description = "Retrieve information of the Kotlin processes after the build execution"
            implementationClass = "io.github.cdsap.kotlinprocess.InfoKotlinProcessPlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/cdsap/InfoKotlinProcess"
    vcsUrl = "https://github.com/cdsap/InfoKotlinProcess"
    tags = listOf("kotlin", "process")
}

publishing {
    repositories {
        maven {
            name = "Snapshots"
            url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")

            credentials {
                username = System.getenv("USERNAME_SNAPSHOT")
                password = System.getenv("PASSWORD_SNAPSHOT")
            }
        }
        maven {
            name = "Release"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")

            credentials {
                username = System.getenv("USERNAME_SNAPSHOT")
                password = System.getenv("PASSWORD_SNAPSHOT")
            }
        }
    }
    publications {
        create<MavenPublication>("kotlinProcessPublication") {
            from(components["java"])
            artifactId = "kotlinprocess"
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            pom {
                scm {
                    connection.set("scm:git:git://github.com/cdsap/InfoKotlinProcess/")
                    url.set("https://github.com/cdsap/InfoKotlinProcess/")
                }
                name.set("InfoKotlinProcess")
                url.set("https://github.com/cdsap/InfoKotlinProcess/")
                description.set(
                    "Retrieve information of the Kotlin process in your Build Scan or console"
                )
                licenses {
                    license {
                        name.set("The MIT License (MIT)")
                        url.set("https://opensource.org/licenses/MIT")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("cdsap")
                        name.set("Inaki Villar")
                    }
                }
            }
        }
    }
}
