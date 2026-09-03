import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-gradle-plugin`
    `maven-publish`
    kotlin("jvm") version "2.1.0"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    mavenCentral()
    google()
}

// AGP must be implementation (not compileOnly) so the plugin can resolve
// BaseExtension, ProcessLibraryManifest, DexArchiveBuilder etc. at runtime.
// The classloader conflict with the main buildscript's AGP is handled by
// Gradle's classloader hierarchy: buildSrc classes are loaded FIRST, and the
// main buildscript classes are loaded in a parent classloader that wins for
// the subprojects. AGP 8.7.3 is used in both places so the version matches.
dependencies {
    compileOnly(gradleApi())
    implementation("com.google.guava:guava:33.6.0-jre")
    implementation("com.android.tools:sdk-common:31.1.1")
    implementation("com.android.tools.build:gradle:8.7.3")
    // Implementation (not compileOnly) so KotlinCompile class is available at runtime
    // when the plugin is applied to subprojects
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0")

    implementation("org.ow2.asm:asm:9.7.1")
    implementation("org.ow2.asm:asm-tree:9.7.1")
}

gradlePlugin {
    plugins {
        create("com.lagradost.cloudstream3.gradle") {
            id = "com.lagradost.cloudstream3.gradle"
            implementationClass = "com.lagradost.cloudstream3.gradle.CloudstreamPlugin"
        }
    }
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.addAll(
            listOf(
                "-Xno-call-assertions",
                "-Xno-param-assertions",
                "-Xno-receiver-assertions"
            )
        )
        jvmTarget.set(JvmTarget.JVM_11)
    }
}
