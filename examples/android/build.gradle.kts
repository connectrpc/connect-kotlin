plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    compileSdkVersion(32)

    defaultConfig {
        applicationId = "build.buf.connect.examples.android"
        minSdkVersion(28)
        targetSdkVersion(32)
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.5.1")

    implementation(project(":okhttp"))
    implementation(project(":examples:generated-google-javalite"))
    implementation(libs.okhttp.core)
}
