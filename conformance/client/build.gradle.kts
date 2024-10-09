plugins {
    kotlin("jvm")
}

tasks {
    compileKotlin {
        compilerOptions {
            // Generated Kotlin code for protobuf uses OptIn annotation
            freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
        }
    }
}

dependencies {
    implementation(project(":okhttp"))
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.protobuf.javalite)
    implementation(libs.okio.core)
    implementation(libs.okhttp.tls)
}
