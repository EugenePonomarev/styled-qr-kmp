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
    .get()

val readmeTemplate = layout.projectDirectory.file("docs/templates/README.md.template")
val podspecTemplate = layout.projectDirectory.file("docs/templates/StyledQrKmp.podspec.template")

val readme = layout.projectDirectory.file("README.md")
val podspec = layout.projectDirectory.file("StyledQrKmp.podspec")

val versionPlaceholder = "{{VERSION_NAME}}"

object VersionedFileRenderer {
    private const val VERSION_PLACEHOLDER = "{{VERSION_NAME}}"

    fun render(templateFile: File, version: String): String {
        val template = templateFile.readText()

        check(VERSION_PLACEHOLDER in template) {
            "${templateFile.name} must contain $VERSION_PLACEHOLDER"
        }

        return template.replace(VERSION_PLACEHOLDER, version)
    }
}

tasks.register("updateVersionedDocs") {
    val releaseVersion = libraryVersion
    val readmeTemplateFile = readmeTemplate.asFile
    val podspecTemplateFile = podspecTemplate.asFile
    val readmeFile = readme.asFile
    val podspecFile = podspec.asFile

    group = "release"
    description = "Regenerates the versioned README.md and CocoaPods specification."

    inputs.files(readmeTemplateFile, podspecTemplateFile)
    inputs.property("version", releaseVersion)
    outputs.files(readmeFile, podspecFile)

    doLast {
        readmeFile.writeText(
            VersionedFileRenderer.render(readmeTemplateFile, releaseVersion),
        )
        podspecFile.writeText(
            VersionedFileRenderer.render(podspecTemplateFile, releaseVersion),
        )
    }
}

tasks.register("checkVersionedDocs") {
    val releaseVersion = libraryVersion
    val readmeTemplateFile = readmeTemplate.asFile
    val podspecTemplateFile = podspecTemplate.asFile
    val readmeFile = readme.asFile
    val podspecFile = podspec.asFile

    group = "verification"
    description = "Fails when generated README.md or podspec differs from VERSION_NAME."

    inputs.files(readmeTemplateFile, podspecTemplateFile, readmeFile, podspecFile)
    inputs.property("version", releaseVersion)

    doLast {
        check(
            readmeFile.readText() ==
                    VersionedFileRenderer.render(readmeTemplateFile, releaseVersion),
        ) {
            "README.md is out of date. Run ./gradlew updateVersionedDocs and commit the result."
        }

        check(
            podspecFile.readText() ==
                    VersionedFileRenderer.render(podspecTemplateFile, releaseVersion),
        ) {
            "StyledQrKmp.podspec is out of date. Run ./gradlew updateVersionedDocs and commit the result."
        }
    }
}

allprojects {
    group = libraryGroup.get()
    version = libraryVersion
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
                        developerConnection.set(
                            "scm:git:ssh://github.com/EugenePonomarev/styled-qr-kmp.git",
                        )
                        url.set("https://github.com/EugenePonomarev/styled-qr-kmp")
                    }
                }
            }
        }
    }
}