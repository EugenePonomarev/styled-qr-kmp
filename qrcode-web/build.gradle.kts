import org.jetbrains.kotlin.gradle.dsl.JsModuleKind

plugins {
    kotlin("multiplatform")
    kotlin("npm-publish") version "3.7.0"
}

kotlin {
    js {
        browser()
        binaries.library()
        generateTypeScriptDefinitions()

        compilerOptions {
            moduleKind.set(JsModuleKind.MODULE_ES)
            target.set("es2015")
        }
    }

    sourceSets {
        jsMain.dependencies {
            implementation(project(":qrcode-core"))
        }

        jsTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

npmPublish {
    // Kotlin downloads Node with the Windows file layout, not bin/node and bin/npm.
    if (System.getProperty("os.name").startsWith("Windows")) {
        nodeBin.set(nodeHome.file("node.exe"))
        npmBin.set(nodeHome.file("node_modules/npm/bin/npm-cli.js"))
    }

    readme.set(rootProject.file("README.md"))

    packages {
        named("js") {
            packageName.set("styled-qr-kmp-web")
            main.set("styled-qr-kmp-qrcode-web.mjs")
            types.set("styled-qr-kmp-qrcode-web.d.mts")

            files {
                from(rootProject.file("LICENSE"))
            }

            packageJson {
                description =
                    "JavaScript API for the dependency-free Styled QR KMP generator."
                license = "MIT"
                homepage = "https://github.com/EugenePonomarev/styled-qr-kmp"
                keywords = listOf(
                    "kotlin",
                    "kotlin-js",
                    "kotlin-multiplatform",
                    "qr-code",
                    "svg",
                )
                repository {
                    type = "git"
                    url = "git+https://github.com/EugenePonomarev/styled-qr-kmp.git"
                }
            }
        }
    }
}

tasks.register("verifyJsNpmPackage") {
    group = "verification"
    description =
        "Checks that the assembled npm package contains the ESM entry point and TypeScript definitions."

    val jsPackageDirectory = layout.buildDirectory.dir("packages/js")

    dependsOn("assembleJsPackage")
    inputs.dir(jsPackageDirectory)

    doLast {
        val packageDirectory = jsPackageDirectory.get().asFile
        val requiredFiles = listOf(
            "package.json",
            "README.md",
            "LICENSE",
            "styled-qr-kmp-qrcode-web.mjs",
            "styled-qr-kmp-qrcode-web.d.mts",
        )
        val missingFiles = requiredFiles.filterNot {
            packageDirectory.resolve(it).isFile
        }

        check(missingFiles.isEmpty()) {
            "npm package is missing: ${missingFiles.joinToString()}"
        }
    }
}