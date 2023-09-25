
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