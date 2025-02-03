plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    compileSdk = 35

    defaultConfig {
        applicationId = "com.connectrpc.examples.android"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    namespace = "com.connectrpc.examples.android"
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintLayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.android.multidex)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.android.material)

    implementation(project(":okhttp"))
    implementation(project(":examples:generated-google-javalite"))
    implementation(libs.okhttp.core)
}
