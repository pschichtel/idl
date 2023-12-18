
plugins {
    id("tel.schich.idl.conventions-base")
    id("tel.schich.idl.conventions-json-gen")
}

repositories {
    mavenCentral()
}

dependencies {
    val kotlinxSerializationVersion = "1.6.2"
    api("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationVersion")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
}