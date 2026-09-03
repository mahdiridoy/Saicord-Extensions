import com.android.build.gradle.BaseExtension
import com.lagradost.cloudstream3.gradle.CloudstreamExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }

    dependencies {
        classpath("com.android.tools.build:gradle:8.7.3")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.0")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        // JitPack for runtime dependencies (NiceHttp, etc.)
        maven("https://jitpack.io")
    }
}

fun Project.cloudstream(configuration: CloudstreamExtension.() -> Unit) = extensions.getByName<CloudstreamExtension>("cloudstream").configuration()

fun Project.android(configuration: BaseExtension.() -> Unit) = extensions.getByName<BaseExtension>("android").configuration()

subprojects {
    apply(plugin = "com.android.library")
    apply(plugin = "kotlin-android")
    apply(plugin = "com.lagradost.cloudstream3.gradle")

    cloudstream {
        // when running through github workflow, GITHUB_REPOSITORY should contain current repository name
        setRepo(System.getenv("GITHUB_REPOSITORY") ?: "https://github.com/mahdiridoy/Saicord-Extensions")
    }

    android {
        namespace = "com.saicord"

        defaultConfig {
            minSdk = 21
            compileSdkVersion(35)
            targetSdk = 35
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }

        tasks.withType<KotlinCompile> {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_1_8) // Required
                freeCompilerArgs.addAll(
                    "-Xno-call-assertions",
                    "-Xno-param-assertions",
                    "-Xno-receiver-assertions"
                )
            }
        }
    }

    dependencies {
        val cloudstream by configurations
        val implementation by configurations
        val compileOnly by configurations

        // Stubs for all Cloudstream classes
        cloudstream("com.lagradost:cloudstream3:pre-release")

        // These dependencies can include any of those which are added by the app,
        // but you don't need to include any of them if you don't need them.
        // https://github.com/recloudstream/cloudstream/blob/master/app/build.gradle
        implementation(kotlin("stdlib")) // Adds Standard Kotlin Features
        implementation("com.github.Blatzar:NiceHttp:0.4.11") // HTTP Lib
        implementation("org.jsoup:jsoup:1.18.3") // HTML Parser
        // jspecify annotations used by CloudStream library for nullability
        compileOnly("org.jspecify:jspecify:1.0.0") // Nullability annotations
        // IMPORTANT: Do not bump Jackson above 2.13.1, as newer versions will
        // break compatibility on older Android devices.
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1") // JSON Parser
    }
}

task<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
