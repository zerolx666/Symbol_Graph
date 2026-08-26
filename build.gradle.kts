import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
    id("org.jetbrains.intellij.platform") version "2.18.1"
}

group = "com.example.symbolgraph"
version = "0.1.3"

// The project property is intentionally outside the repository so Rider/Gradle does not
// fill the system drive with build outputs. Gradle's dependency cache is configured separately
// through GRADLE_USER_HOME (see build-off-c.ps1).
providers.gradleProperty("symbolGraphBuildDir").orNull?.let { externalBuildDir ->
    layout.buildDirectory.set(file(externalBuildDir))
}

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    intellijPlatform {
        rider("2026.1.3") {
            useInstaller.set(false)
        }
    }
}

kotlin {
    // JDK 21 runs the compiler; Java 17 bytecode also runs on Rider 2026.1's JBR 21.
    jvmToolchain(21)
}

tasks {
    patchPluginXml {
        sinceBuild.set("261")
        untilBuild.set("261.*")
    }

    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    withType<KotlinCompile> {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
    }
}
