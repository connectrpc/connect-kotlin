plugins {
    kotlin("jvm")
}

// This base project contains generated code for the lite runtime
// and depends on the Google Protobuf Java Lite runtime.
// The main client logic is implemented in terms of generated
// code for that lite runtime.
//
// The non-lite runtime excludes the Google Protobuf Java Lite
// runtime and instead uses the full Java runtime. It then can
// adapt from the lite-runtime-generated code by serializing to
// bytes and then de-serializing into non-lite-generated types.

sourceSets {
    main {
        java {
            srcDir("build/generated/sources/bufgen")
        }
    }
}

tasks {
    compileKotlin {
        kotlinOptions {
            // Generated Kotlin code for protobufs uses RequiresOptIn annotation
            freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
        }
    }
}

dependencies {
    implementation(project(":okhttp"))
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.protobuf.kotlin)
    implementation(libs.protobuf.javalite)
    implementation(libs.okio.core)
    implementation(libs.okhttp.tls)
}
