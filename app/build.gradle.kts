plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.compose") version "2.0.21"
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

    // Kotlin compiler agora é gerido pelo plugin Compose
    composeOptions {
        // kotlinCompilerExtensionVersion = "1.6.0"  <-- opcional com plugin novo
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(libs.places)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.ui.graphics)
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Material 3
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")

    // Lifecycle (opcional, mas útil)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

    // Para timestamps (opcional)
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    implementation("androidx.compose.ui:ui:1.7.5")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.5")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
    implementation("androidx.navigation:navigation-compose:2.7.3")
}
