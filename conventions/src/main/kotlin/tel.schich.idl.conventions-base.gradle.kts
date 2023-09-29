import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    publishing
    `maven-publish`
    idea
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of("8"))
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

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
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