plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

group = "tel.schich.idl.plugin"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    gradlePluginPortal()
    mavenLocal()
}

dependencies {
    api(project(":runner"))
}

gradlePlugin {
    plugins {
        create("idlPlugin") {
            id = "tel.schich.idl.plugin"
            implementationClass = "tel.schich.idl.plugin.IdlPlugin"
        }
    }
}
