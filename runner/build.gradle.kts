plugins {
    id("tel.schich.idl.conventions-base")
    application
}

dependencies {
    implementation(project(":core"))
    implementation("com.github.ajalt.clikt:clikt:4.2.1")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.16.0")
}