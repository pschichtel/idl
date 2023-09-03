plugins {
    id("tel.schich.idl.conventions-base")
    id("tel.schich.idl.conventions-json-gen")
    application
}

version = "1.0-SNAPSHOT"
dependencies {
    implementation(project(":core"))
    implementation(project(":runner"))
}