plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    compileSdkVersion(32)

    defaultConfig {
        applicationId = "com.connectrpc.examples.android"
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
    namespace = "com.connectrpc.examples.android"
}

dependencies {
    // TODO: add deps to libs.versions.toml
    implementation("androidx.appcompat:appcompat:1.5.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("com.android.support:multidex:1.0.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.5.1")
    implementation("com.google.android.material:material:1.4.0")

    implementation(project(":okhttp"))
    implementation(project(":examples:generated-google-javalite"))
    implementation(libs.okhttp.core)
}
