import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// API key is read from local.properties (git-ignored): ANTHROPIC_API_KEY=sk-ant-...
// NOTE: a key embedded in an APK can be extracted. For anything beyond personal use,
// route requests through your own backend instead.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.example.hebrewassistant"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.hebrewassistant"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1"
        buildConfigField("String", "ANTHROPIC_API_KEY", "\"${localProps.getProperty("ANTHROPIC_API_KEY", "")}\"")
        buildConfigField("String", "CLAUDE_MODEL", "\"claude-haiku-5-5\"")
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.mlkit.translate)
    implementation(libs.okhttp)
}
