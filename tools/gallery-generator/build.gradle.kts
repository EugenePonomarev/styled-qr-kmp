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

tasks.register<JavaExec>("assembleDocsSite") {
    group = "documentation"
    description = "Builds the initial static documentation site."

    dependsOn(tasks.named("classes"))
    dependsOn(":checkGallery")
    dependsOn(":checkVersionedDocs")

    val siteOutputDirectory = rootProject.layout.buildDirectory.dir("docs-site")
    val siteStylesheet = rootProject.layout.projectDirectory.file("docs/site/style.css")
    val siteVersion = providers.gradleProperty("VERSION_NAME").get()

    mainClass.set(
        "io.github.eugeneponomarev.styledqr.gallery.DocsSiteGeneratorKt",
    )
    classpath = sourceSets.main.get().runtimeClasspath

    inputs.files(sourceSets.main.get().allSource)
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.dir(committedGalleryDirectory)
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.property("version", siteVersion)
    inputs.file(siteStylesheet)
        .withPathSensitivity(PathSensitivity.RELATIVE)

    args(
        committedGalleryDirectory.asFile.absolutePath,
        siteOutputDirectory.get().asFile.absolutePath,
        siteStylesheet.asFile.absolutePath,
        siteVersion,
    )

    outputs.dir(siteOutputDirectory)
    outputs.upToDateWhen { false }
}

tasks.register<JavaExec>("verifyDocsSite") {
    group = "verification"
    description = "Checks the assembled documentation site."

    dependsOn("assembleDocsSite")

    val siteOutputDirectory = rootProject.layout.buildDirectory.dir("docs-site")

    mainClass.set(
        "io.github.eugeneponomarev.styledqr.gallery.DocsSiteCheckKt",
    )
    classpath = sourceSets.main.get().runtimeClasspath

    inputs.dir(siteOutputDirectory)
        .withPathSensitivity(PathSensitivity.RELATIVE)

    args(siteOutputDirectory.get().asFile.absolutePath)
}