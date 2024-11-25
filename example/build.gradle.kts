plugins {
    kotlin("jvm") version("2.0.21") apply(false)
    kotlin("plugin.serialization") version("2.0.21") apply(false)
    id("tel.schich.idl.plugin") apply(false)
}

allprojects {
    repositories {
        mavenCentral()
    }

    group = "tel.schich.idl.example"
    version = "1.0-SNAPSHOT"
}
