plugins {
    kotlin("jvm")
}

dependencies {
    implementation(libs.okio.core)
    implementation(libs.okhttp.tls)
    implementation(libs.junit)
    implementation(libs.assertj)
    implementation(libs.testcontainers)
    implementation(project(":okhttp"))
}
