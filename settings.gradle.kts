
rootProject.name = "kotlin-idl"

include(
    ":core",
    ":runner",
    ":generator:openapi",
    ":generator:kotlin",
    ":plugin",
)

includeBuild("example")

pluginManagement {
    includeBuild("conventions")
    repositories {
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}
