import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("maven-publish")
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    // Lets the QR core and SVG renderer run in local unit tests without an Android device.
    jvm()

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    // JitPack builds on Linux, so Apple binaries are exported by the macOS release workflow.
    if (System.getenv("JITPACK") != "true") {
        val xcframework = XCFramework("StyledQrKmp")

        listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
            target.binaries.framework {
                baseName = "StyledQrKmp"
                isStatic = true
                binaryOption("bundleId", "io.github.eugeneponomarev.styledqr")
                xcframework.add(this)
            }
        }
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "io.github.eugeneponomarev.styledqr"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    // Android defaults to Java 8 otherwise, which conflicts with Kotlin JVM 17.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
