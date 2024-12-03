plugins {
    id("tel.schich.idl.conventions-base")
    application
}

dependencies {
    implementation(project(":core"))
    implementation("com.github.ajalt.clikt:clikt:5.0.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.2")
}