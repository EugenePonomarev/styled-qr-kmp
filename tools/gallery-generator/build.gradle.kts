plugins {
    kotlin("jvm")
    application
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":qrcode-core"))

    testImplementation(kotlin("test-junit"))
}

val galleryMainClass = "io.github.eugeneponomarev.styledqr.gallery.GalleryGeneratorKt"
val logoInputsDirectory = rootProject.layout.projectDirectory.dir("docs/gallery-inputs")
val committedGalleryDirectory = rootProject.layout.projectDirectory.dir("docs/gallery")

application {
    mainClass.set(galleryMainClass)
}

tasks.test {
    inputs.dir(logoInputsDirectory)
        .withPathSensitivity(PathSensitivity.RELATIVE)

    systemProperty(
        "gallery.logoInputs",
        logoInputsDirectory.asFile.absolutePath,
    )
}

val generatorTaskNames = setOf(
    "generateGalleryPreview",
    "writeGallery",
    "verifyGallery",
)

tasks.withType<JavaExec>().configureEach {
    if (name in generatorTaskNames) {
        dependsOn(tasks.named("classes"))

        mainClass.set(galleryMainClass)
        classpath = sourceSets.main.get().runtimeClasspath

        inputs.files(sourceSets.main.get().allSource)
            .withPathSensitivity(PathSensitivity.RELATIVE)

        inputs.dir(logoInputsDirectory)
            .withPathSensitivity(PathSensitivity.RELATIVE)
    }
}

tasks.register<JavaExec>("generateGalleryPreview") {
    group = "documentation"
    description = "Generates all gallery assets under the module's build directory."

    val previewDirectory = layout.buildDirectory.dir("gallery-preview")

    args(
        "update",
        logoInputsDirectory.asFile.absolutePath,
        previewDirectory.get().asFile.absolutePath,
    )

    outputs.dir(previewDirectory)
    outputs.upToDateWhen { false }
}

val writeGalleryTask = tasks.register<JavaExec>("writeGallery") {
    group = "documentation"
    description = "Updates the managed SVG gallery and manifest in docs/gallery."

    args(
        "update",
        logoInputsDirectory.asFile.absolutePath,
        committedGalleryDirectory.asFile.absolutePath,
    )

    outputs.dir(committedGalleryDirectory)

    // An explicit update must also inspect unexpected files.
    outputs.upToDateWhen { false }
}

tasks.register<JavaExec>("verifyGallery") {
    group = "verification"
    description = "Checks gallery bytes and file set without updating docs/gallery."

    val candidateDirectory = layout.buildDirectory.dir("gallery-check")

    args(
        "check",
        logoInputsDirectory.asFile.absolutePath,
        committedGalleryDirectory.asFile.absolutePath,
        candidateDirectory.get().asFile.absolutePath,
    )

    inputs.files(committedGalleryDirectory)
        .withPathSensitivity(PathSensitivity.RELATIVE)

    outputs.dir(candidateDirectory)
    outputs.upToDateWhen { false }

    mustRunAfter(writeGalleryTask)
}