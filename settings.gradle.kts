
rootProject.name = "kotlin-idl"

include(
    "core",
    "runner",
)

pluginManagement {
    includeBuild("conventions")
    repositories {
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}