plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    kotlin("kapt")
    // Hilt Gradle plugin removed temporarily to avoid plugin tasks that can fail in some environments.
    // We still keep the Hilt runtime and kapt compiler dependency; re-add the plugin when Hilt plugin integration is needed.
}

android {
    namespace = "com.example.smartlist"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.smartlist"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        // Compose Compiler 1.5.3 is compatible with Kotlin 1.9.10 (see AndroidX compatibility map).
        // Set an explicit, published compiler extension version to avoid Gradle selecting an
        // older incompatible default.
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    packagingOptions {
        resources.excludes += setOf("META-INF/AL2.0", "META-INF/LGPL2.1")
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.01.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.2.0")

    // ViewModel + Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    // Compose integration for ViewModel (provides viewModel() in composables)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    // Ensure a compatible JavaPoet version is available for annotation processors (fixes NoSuchMethodError in some environments)
    implementation("com.squareup:javapoet:1.13.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.8.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    // Compose UI testing (explicit) - version chosen to match Compose 1.5.x
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.0")
}
