import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-gradle-plugin`
    `maven-publish`
    kotlin("jvm") version "2.1.0"
}

group = "com.lagradost.cloudstream3"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
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

repositories {
    mavenCentral()
    google()
}

dependencies {
    compileOnly(gradleApi())
    compileOnly("com.google.guava:guava:33.6.0-jre")
    compileOnly("com.android.tools:sdk-common:32.1.1")
    compileOnly("com.android.tools.build:gradle:8.7.3")
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0")

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
