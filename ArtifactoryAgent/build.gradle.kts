import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
    id("com.gradleup.shadow") version "9.0.0-beta4"
}

group = "icu.lama"
version = "2.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Bytecode manipulation
    implementation("org.javassist:javassist:3.30.2-GA")

    // Annotations
    implementation("org.jetbrains:annotations:26.0.1")

    // Logging (for enhanced debugging)
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("ch.qos.logback:logback-classic:1.5.12")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
}

// Use Java 25 toolchain but compile for Java 21 compatibility
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile> {
    // Compile for Java 21 target (Artifactory uses JDK 21)
    options.release.set(21)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<ShadowJar> {
    manifest {
        attributes["Premain-Class"] = "icu.lama.artifactory.agent.AgentMain"
        attributes["Main-Class"] = "icu.lama.artifactory.agent.AgentMain"
        attributes["Can-Redefine-Classes"] = true
        attributes["Can-Retransform-Classes"] = true
    }
}