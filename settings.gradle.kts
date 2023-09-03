
rootProject.name = "kotlin-idl"

include(
    "core",
    "runner",
    ":generator:openapi",
)

pluginManagement {
    includeBuild("conventions")
    repositories {
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}