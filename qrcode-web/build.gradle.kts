import org.jetbrains.kotlin.gradle.dsl.JsModuleKind

plugins {
    kotlin("multiplatform")
}

kotlin {
    js {
        browser()
        binaries.executable()
        generateTypeScriptDefinitions()

        compilerOptions {
            moduleKind.set(JsModuleKind.MODULE_ES)
            target.set("es2015")
        }

        compilations["main"].packageJson {
            customField("name", "styled-qr-kmp-web")
            customField(
                "description",
                "JavaScript API for the dependency-free Styled QR KMP generator.",
            )
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