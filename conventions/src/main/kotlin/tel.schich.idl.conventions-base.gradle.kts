import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    publishing
    `maven-publish`
    idea
}

repositories {
    mavenCentral()
}

val jvmTarget = 8
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmTarget))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
}

dependencies {
    testImplementation(kotlin("test"))
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(jvmTarget)
}

publishing {
    publications {
        register<MavenPublication>("maven") {
            groupId = "${project.group}"
            artifactId = project.name

            from(components["java"])
        }
    }
}