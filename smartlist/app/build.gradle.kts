plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.smartlist"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    buildFeatures {
        compose = true
    }

    namespace = "com.example.smartlist"
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.7.2")
    implementation("androidx.compose.ui:ui:1.6.0")
    implementation("androidx.compose.material:material:1.6.0")
    // Icons (extended) for Compose
    implementation("androidx.compose.material:material-icons-extended:1.6.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.0")
    implementation(kotlin("stdlib"))
}
