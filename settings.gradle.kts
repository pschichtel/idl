
rootProject.name = "kotlin-idl"

include(
    "core",
    "runner",
    ":generator:openapi",
    ":generator:kotlin",
)

pluginManagement {
    includeBuild("conventions")
    repositories {
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}