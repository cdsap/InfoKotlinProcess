import org.gradle.plugin.compatibility.compatibility

plugins {
    `java-gradle-plugin`
    `maven-publish`
    `kotlin-dsl`
    alias(libs.plugins.pluginPublish)
    alias(libs.plugins.ktlint)
}

group = "io.github.cdsap"
version = "0.3.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(libs.cdsap.jdkToolsParser)
    implementation(libs.cdsap.commandlineValueSource)
    implementation(libs.picnic)
    compileOnly(libs.develocity.gradlePlugin)
    testImplementation(libs.junit)
}
tasks.withType<Test>().configureEach {
    filter {

        if (project.hasProperty("excludeTests")) {
            excludeTest(project.property("excludeTests").toString(), "")
        }
    }
}
gradlePlugin {
    website.set("https://github.com/cdsap/InfoKotlinProcess")
    vcsUrl.set("https://github.com/cdsap/InfoKotlinProcess")
    plugins {
        create("InfoKotlinProcessPlugin") {
            id = "io.github.cdsap.kotlinprocess"
            displayName = "Info Kotlin Processes"
            description = "Retrieve information of the Kotlin processes after the build execution"
            implementationClass = "io.github.cdsap.kotlinprocess.InfoKotlinProcessPlugin"
            tags.set(listOf("kotlin", "process"))
            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
    }
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
                    "Retrieve information of the Kotlin process in your Build Scan or console",
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
