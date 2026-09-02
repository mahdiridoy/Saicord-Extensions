plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("cloudstream")
}

android {
    namespace = "com.saicord.provider"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    compileOnly("com.lagradost.cloudstream3:cloudstream:3.1.15")
}

cloudstream {
    // Name of your extension (shown in app)
    name = "Saicord"
    // Version code (increment on each release)
    versionCode = 1
    // Version name (shown in app)
    versionName = "1.0.0"
    // Your GitHub username
    author = "mahdiridoy"
    // Description of your extension
    description = "Saicord Bengali & Hindi Dubbed Movies and Series"
    // Icon for your extension (optional)
    iconUrl = ""
    // Path to your icon (optional)
    iconPath = "assets/icon.png"
    // Repository URL
    repoUrl = "https://github.com/mahdiridoy/Saicord-Extensions"
    // Content type
    contentType = ContentType.Movie
    // Supported languages
    languages = listOf("bn", "hi")
}
