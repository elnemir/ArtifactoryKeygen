import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "2.1.20"
    id("com.gradleup.shadow") version "9.0.0-beta4"
    application
}

group = "icu.lama"
version = "2.0-SNAPSHOT"

application {
    mainClass.set("icu.lama.artifactory.keygen.KeygenKt")
}

repositories {
    mavenCentral()
}

dependencies {
    // CLI and utilities
    implementation("commons-cli:commons-cli:1.9.0")
    implementation("commons-codec:commons-codec:1.17.1")

    // Cryptography - BouncyCastle
    implementation("org.bouncycastle:bcprov-jdk18on:1.79")

    // JSON processing - migrated from Jackson 1.x to 2.x for Java 25 compatibility
    implementation("com.fasterxml.jackson.core:jackson-core:2.18.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.18.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")

    // JFrog proprietary libraries
    // NOTE: Extracted from Artifactory Enterprise 7.125.10
    implementation(files("./libs/artifactory-addons-manager-7.125.10.jar"))

    // YAML processing
    implementation("org.yaml:snakeyaml:2.3")

    // HTTP client
    implementation("org.apache.httpcomponents:httpclient:4.5.14")
    implementation("org.apache.httpcomponents:httpcore:4.4.16")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("ch.qos.logback:logback-classic:1.5.12")

    // Testing dependencies
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_23)
    }
}

tasks.withType<JavaCompile> {
    options.release.set(23)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<ShadowJar> {
    manifest {
        attributes["Main-Class"] = "icu.lama.artifactory.keygen.KeygenKt"
    }
}