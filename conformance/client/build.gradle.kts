plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":okhttp"))
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.protobuf.javalite)
    implementation(libs.okio.core)
    implementation(libs.okhttp.tls)
}
