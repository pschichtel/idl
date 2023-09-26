import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import tel.schich.idl.plugin.tasks.GenerateKotlinTask
import tel.schich.idl.plugin.tasks.GenerateOpenApiTask

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("tel.schich.idl.plugin")
    application
    idea
}

application {
    mainClass.set("tel.schich.idl.example.cvg.MainKt")
}

dependencies {
    val kotlinxSerializationVersion = "1.6.0"
    api("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationVersion")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
    val jacksonVersion = "2.15.2"
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
}

val generateOpenApi by tasks.registering(GenerateOpenApiTask::class) {
}


val generateKotlin by tasks.registering(GenerateKotlinTask::class) {
    dependsOn(generateOpenApi)

    sourceSets {
        main {
            java {
                srcDir(outputs)
            }
        }
    }

    idea.module.generatedSourceDirs.add(output.get().asFile)

    doLast {
        output
    }
}

tasks.withType<KotlinCompile>().configureEach {
    dependsOn(generateKotlin)
}