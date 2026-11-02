plugins {
    id("com.android.application")
}

android {
    namespace = "edu.austincc.itse1333.breakout"
    compileSdk = 34

    defaultConfig {
        applicationId = "edu.austincc.itse1333.breakout"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// NOTE: no dependencies block — and that's on purpose.
// This game draws everything itself with SurfaceView + Canvas + Paint,
// which all live in the core Android framework. No AppCompat, no Material,
// no layout XML. Fewer dependencies = faster builds and less magic.
