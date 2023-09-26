
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

//plugins {
//    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
//}
