
rootProject.name = "kotlin-idl-examples"

include(
    ":cvg",
    ":simple",
)

pluginManagement {
    includeBuild("../conventions")
    includeBuild("..")
    repositories {
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}