plugins {
    id("tel.schich.idl.conventions-base")
    id("tel.schich.idl.conventions-json-gen")
    application
}

dependencies {
    implementation(project(":core"))
    implementation(project(":runner"))
    implementation("com.ibm.icu:icu4j:74.1")
}