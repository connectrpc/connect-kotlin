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
    compileKotlin {
        kotlinOptions {
            // Generated Kotlin code for protobufs uses OptIn annotation
            freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
        }
    }
    shadowJar {
        archiveBaseName.set("shadow")
        manifest {
            attributes(mapOf("Main-Class" to "com.connectrpc.conformance.client.java.MainKt"))
        }
    }
    build {
        dependsOn(shadowJar)
    }
}

// This project contains an alternate copy of the generated
// types, generated for the non-lite runtime.
sourceSets {
    main {
        java {
            srcDir("build/generated/sources/bufgen")
        }
    }
}

dependencies {
    implementation(project(":conformance:client")) {
        exclude(group = "com.google.protobuf", module = "protobuf-javalite")
    }
    implementation(project(":extensions:google-java"))
    implementation(project(":okhttp"))
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.protobuf.kotlin)
    implementation(libs.protobuf.java)
    implementation(libs.okio.core)
    implementation(libs.okhttp.tls)
}
