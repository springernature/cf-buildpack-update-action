import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.10"
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "com.springernature"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.lordcodes.turtle:turtle:0.5.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("buildpack-update-action")
    mergeServiceFiles()
    manifest {
        attributes(mapOf("Main-Class" to "com.springernature.MainKt"))
    }
}