buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("gradle.plugin.com.github.johnrengelman:shadow:7.1.2")
    }
}

plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow").version("7.1.2")
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
    implementation(libs.protobuf.kotlin)
    implementation(libs.okio.core)
    implementation(libs.okhttp.tls)
}
