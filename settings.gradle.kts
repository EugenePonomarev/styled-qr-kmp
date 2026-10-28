pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "styled-qr-kmp"
include(":qrcode-core")
include(":qrcode-android-view")
include(":qrcode-compose")
include(":qrcode-web")
include(":gallery-generator")
project(":gallery-generator").projectDir = file("tools/gallery-generator")