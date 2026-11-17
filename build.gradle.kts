import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "2.4.21"
    id("com.gradleup.shadow") version "9.4.1"
}

group = "com.springernature"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.18")
    implementation("com.lordcodes.turtle:turtle:0.10.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.21.8")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.21.8")
    implementation("io.github.z4kn4fein:semver:3.1.0")

    runtimeOnly("ch.qos.logback:logback-classic:1.6.6")

    runtimeOnly("org.junit.platform:junit-platform-launcher:6.1.0")
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.amshove.kluent:kluent:1.73")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.1.3")
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(JavaVersion.VERSION_25.majorVersion))
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