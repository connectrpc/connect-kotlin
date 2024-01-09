buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath(libs.shadowjar)
    }
}

plugins {
    kotlin("jvm")
    alias(libs.plugins.shadowjar)
}

tasks {
    compileKotlin {
        kotlinOptions {
            // Generated Kotlin code for protobuf uses RequiresOptIn annotation
            freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
        }
    }
    shadowJar {
        archiveFileName.set("conformance-client-javalite.jar")
        manifest {
            attributes(mapOf("Main-Class" to "com.connectrpc.conformance.client.javalite.MainKt"))
        }
    }
    build {
        dependsOn(shadowJar)
    }
}

sourceSets {
    main {
        java {
            srcDir("build/generated/sources/bufgen")
        }
    }
}

dependencies {
    implementation(project(":conformance:client"))
    implementation(project(":extensions:google-javalite"))
    implementation(project(":okhttp"))
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.protobuf.kotlinlite)
    implementation(libs.protobuf.javalite)
    implementation(libs.okio.core)
    implementation(libs.okhttp.tls)
}
