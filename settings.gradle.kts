
rootProject.name = "kotlin-idl"

include(
    ":core",
    ":runner",
    ":generator:openapi",
    ":generator:kotlin",
)

includeBuild("example")
includeBuild("plugin")

pluginManagement {
    includeBuild("conventions")
    repositories {
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}