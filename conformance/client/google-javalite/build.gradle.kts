plugins {
    kotlin("jvm")
    application
}

application {
    mainClass.set("com.connectrpc.conformance.client.javalite.MainKt")
}

tasks {
    compileKotlin {
        kotlinOptions {
            // Generated Kotlin code for protobuf uses RequiresOptIn annotation
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
    implementation(project(":conformance:client"))
    implementation(project(":extensions:google-javalite"))
    implementation(project(":okhttp"))
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.protobuf.kotlinlite)
    implementation(libs.protobuf.javalite)
    implementation(libs.okio.core)
    implementation(libs.okhttp.tls)
}
