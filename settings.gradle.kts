
rootProject.name = "kotlin-idl"

include(
    "core",
    "runner",
    "generator",
)

pluginManagement {
    includeBuild("conventions")
    repositories {
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}