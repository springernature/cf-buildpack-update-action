import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.9.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.springernature"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("com.lordcodes.turtle:turtle:0.9.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.20.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.20.0")
    implementation("net.swiftzer.semver:semver:1.3.0")

    runtimeOnly("ch.qos.logback:logback-classic:1.4.14")

    runtimeOnly("org.junit.platform:junit-platform-launcher:1.13.4")
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.amshove.kluent:kluent:1.73")

    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", "5.8.2")
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(JavaVersion.VERSION_17.majorVersion))
    }
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("cf-buildpack-update-action")
    mergeServiceFiles()
    manifest {
        attributes(mapOf("Main-Class" to "com.springernature.newversion.MainKt"))
    }
}

tasks {
    test {
        useJUnitPlatform()
    }
}