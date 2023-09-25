import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    publishing
    `maven-publish`
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of("8"))
    }
}
dependencies {
    testImplementation(kotlin("test"))
    implementation(platform("org.jetbrains.kotlinx:kotlinx-serialization-bom:1.6.0"))
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