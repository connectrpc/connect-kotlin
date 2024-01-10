plugins {
    kotlin("jvm")
    application
}

application {
    mainClass.set("com.connectrpc.conformance.client.java.MainKt")
}

tasks {
    compileKotlin {
        kotlinOptions {
            // Generated Kotlin code for protobuf uses OptIn annotation
            freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
        }
    }
    jar {
        manifest {
            attributes(mapOf("Main-Class" to application.mainClass.get()))
        }
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
            exclude("META-INF/**/*")
        }
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
    implementation(project(":conformance:client")) {
        // Shared module depends on javalite, just for some core
        // classes that are shared across both java and javalite
        // runtimes, like ByteString and MessageLite. We must
        // exclude it here to avoid any classpath ambiguity since
        // we pull in the full runtime for this module.
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
