
plugins {
    id("tel.schich.idl.conventions-base")
    id("tel.schich.idl.conventions-json-gen")
}

version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-serialization-core")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json")
}