import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

plugins {
    kotlin("multiplatform") version "2.2.20" apply false
    kotlin("android") version "2.2.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20" apply false
    id("com.android.library") version "8.12.3" apply false
    id("com.android.application") version "8.12.3" apply false
}

val libraryGroup = providers
    .gradleProperty("POM_GROUP_ID")
    .orElse("com.github.EugenePonomarev.styled-qr-kmp")
val libraryVersion = providers
    .gradleProperty("VERSION_NAME")
    .orElse("0.2.2")

allprojects {
    group = libraryGroup.get()
    version = libraryVersion.get()
}

subprojects {
    pluginManager.withPlugin("maven-publish") {
        extensions.configure<PublishingExtension> {
            publications.withType<MavenPublication>().configureEach {
                pom {
                    name.set("Styled QR KMP · ${project.name}")
                    description.set(
                        "Dependency-free Kotlin Multiplatform QR-code generator with Android View, Compose, and iOS UI adapters.",
                    )
                    url.set("https://github.com/EugenePonomarev/styled-qr-kmp")

                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/license/mit/")
                            distribution.set("repo")
                        }
                    }

                    developers {
                        developer {
                            id.set("EugenePonomarev")
                            name.set("Evgenii Ponomarev")
                            email.set("eugeneponomarevdev@gmail.com")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/EugenePonomarev/styled-qr-kmp.git")
                        developerConnection.set("scm:git:ssh://github.com/EugenePonomarev/styled-qr-kmp.git")
                        url.set("https://github.com/EugenePonomarev/styled-qr-kmp")
                    }
                }
            }
        }
    }
}
