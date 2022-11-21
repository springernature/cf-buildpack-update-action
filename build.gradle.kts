import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.7.21"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "com.springernature"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.4")
    implementation("com.lordcodes.turtle:turtle:0.8.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.14.0")
    implementation("net.swiftzer.semver:semver:1.2.0")

    runtimeOnly("ch.qos.logback:logback-classic:1.4.5")

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.amshove.kluent:kluent:1.72")

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