plugins {
    id("com.android.application")
    kotlin("android")
    // The compose compiler plugin is tied to your Kotlin version.
    // We'll keep the version you have for now.kotlin("plugin.compose") version "2.0.21"
}

android {
    namespace = "com.griffith.diaryfour"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.griffith.diaryfour"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        // The kotlinCompilerExtensionVersion is managed by the compose plugin
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // Platform for Compose BOM - This manages all compose library versions
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // AndroidX & KTX
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.9.3") // Using the single latest version

    // Compose UI - No versions needed, BOM handles them
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.material:material-icons-extended")

    // UI Tools for Preview
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.3")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

    // Kotlinx Datetime
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0") // Using the single latest version

    // Google Places (from your version catalog)
    implementation(libs.places)

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}