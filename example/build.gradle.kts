plugins {
    kotlin("jvm") version("1.9.25") apply(false)
    kotlin("plugin.serialization") version("1.9.25") apply(false)
    id("tel.schich.idl.plugin") apply(false)
}

allprojects {
    repositories {
        mavenCentral()
    }

    group = "tel.schich.idl.example"
    version = "1.0-SNAPSHOT"
}
