plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "dz.zakary.grd"
    compileSdk = 35

    defaultConfig {
        // Separate package so this Henkel edition installs alongside the older ZAKARY GRD app.
        applicationId = "dz.zakary.grd.henkel"
        minSdk = 23
        targetSdk = 35
        versionCode = 2
        versionName = "1.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
