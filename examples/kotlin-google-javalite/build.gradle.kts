plugins {
    application
    kotlin("jvm")
}

application {
    mainClass.set("build.buf.connect.examples.kotlin.Main")
}

tasks {
    jar {
        manifest {
            attributes(mapOf("Main-Class" to application.mainClass.get()))
        }
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
            exclude("META-INF/**/*")
        }
    }
}

dependencies {
    implementation(project(":examples:generated-google-javalite"))
    implementation(project(":okhttp"))
    implementation(libs.okhttp.core)
    implementation(libs.kotlin.coroutines.core)
}
