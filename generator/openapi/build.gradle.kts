plugins {
    id("tel.schich.idl.conventions-base")
    application
}

version = "1.0-SNAPSHOT"
dependencies {
    implementation(project(":core"))
    implementation(project(":runner"))
}