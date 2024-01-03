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
    shadowJar {
        archiveBaseName.set("shadow")
        manifest {
            attributes(mapOf("Main-Class" to "com.connectrpc.conformance.client.javalite.MainKt"))
        }
    }
    build {
        dependsOn(shadowJar)
    }
}

dependencies {
    implementation(project(":conformance:client"))
    implementation(project(":extensions:google-javalite"))
    implementation(project(":okhttp"))
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.protobuf.kotlinlite)
    implementation(libs.okio.core)
    implementation(libs.okhttp.tls)
}
