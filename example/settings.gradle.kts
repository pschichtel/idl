
rootProject.name = "kotlin-idl-examples"

include(
    ":cvg",
    ":simple",
)

pluginManagement {
    includeBuild("../conventions")
    includeBuild("../plugin")
    repositories {
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}